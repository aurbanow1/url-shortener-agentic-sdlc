package dev.urlshort.click;

// QA-only instrumentation, compiled outside src/. Product bytecode is unchanged.
import dev.urlshort.UrlshortApplication;
import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;
import tools.jackson.databind.json.JsonMapper;

public class QaRetentionRunner {
    static final JsonMapper JSON = JsonMapper.builder().build();
    static final MovingClock CLOCK = new MovingClock();
    static final AtomicBoolean FAIL = new AtomicBoolean();
    static final AtomicBoolean ENTERED = new AtomicBoolean();
    static Connection held;
    static byte[] oldSalt;
    static CompletableFuture<Void> running;
    static class MovingClock extends Clock {
        volatile Instant now = Instant.parse(System.getProperty("qa.instant", "2026-12-15T12:00:00Z"));
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }
    @Configuration(proxyBeanMethods = false)
    static class QaConfig {
        @Bean @Primary Clock qaClock() { return CLOCK; }
        @Bean static BeanPostProcessor qaDataSourceHook() {
            return new BeanPostProcessor() {
                public Object postProcessAfterInitialization(Object bean, String name) {
                    if (!(bean instanceof DataSource source)) return bean;
                    return Proxy.newProxyInstance(DataSource.class.getClassLoader(), new Class<?>[]{DataSource.class},
                        (proxy, method, args) -> {
                            Object value = invoke(source, method, args);
                            if (value instanceof Connection connection) return wrap(connection);
                            return value;
                        });
                }
            };
        }
    }
    static Object invoke(Object target, Method method, Object[] args) throws Throwable {
        try { return method.invoke(target, args); }
        catch (InvocationTargetException e) { throw e.getCause(); }
    }
    static Connection wrap(Connection connection) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
            (proxy, method, args) -> {
                boolean purge = method.getName().equals("prepareStatement") && args[0] instanceof String sql
                    && sql.toUpperCase(Locale.ROOT).startsWith("DELETE FROM CLICK WHERE CLICKED_ON");
                if (purge && FAIL.getAndSet(false)) throw new SQLException("QA_PURGE_DRIVER_SECRET_CANARY");
                Object result = invoke(connection, method, args);
                if (purge && result instanceof PreparedStatement statement) {
                    return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(), new Class<?>[]{PreparedStatement.class},
                        (p, m, a) -> {
                            if (m.getName().equals("executeUpdate")) ENTERED.set(true);
                            return invoke(statement, m, a);
                        });
                }
                return result;
            });
    }
    static Object sql(Connection connection, String text) throws Exception {
        try (Statement statement = connection.createStatement()) {
            if (!statement.execute(text)) return Map.of("updated", statement.getUpdateCount());
            try (ResultSet rs = statement.getResultSet()) {
                List<Map<String,Object>> rows = new ArrayList<>();
                ResultSetMetaData md = rs.getMetaData();
                while (rs.next()) {
                    Map<String,Object> row = new LinkedHashMap<>();
                    for (int i=1; i<=md.getColumnCount(); i++) row.put(md.getColumnLabel(i).toLowerCase(Locale.ROOT), rs.getString(i));
                    rows.add(row);
                }
                return rows;
            }
        }
    }
    static Object command(Map<String,Object> command, ConfigurableApplicationContext ctx) throws Exception {
        String op = (String) command.get("op");
        return switch (op) {
            case "clock" -> { CLOCK.now=Instant.parse((String)command.get("instant")); yield CLOCK.now.toString(); }
            case "sql" -> { try (Connection c=ctx.getBean(DataSource.class).getConnection()) { yield sql(c,(String)command.get("sql")); } }
            case "settle" -> { ctx.getBean(ClickRecorder.class).settle(); yield "settled"; }
            case "purge" -> { ctx.getBean(ClickPurge.class).runNow(); yield "completed"; }
            case "fail" -> { FAIL.set(true); yield "next physical purge prepareStatement throws SQLException"; }
            case "hold" -> {
                held=ctx.getBean(DataSource.class).getConnection(); held.setAutoCommit(false);
                yield sql(held,"DELETE FROM click WHERE link_id="+command.get("linkId")+" AND clicked_on < DATE '"+command.get("cutoff")+"'");
            }
            case "purgeAsync" -> {
                ENTERED.set(false);
                running=CompletableFuture.runAsync(() -> { try {ctx.getBean(ClickPurge.class).runNow();} catch(Exception e) {throw new CompletionException(e);} });
                yield "submitted";
            }
            case "state" -> Map.of("enteredExecuteUpdate",ENTERED.get(),"finished",running != null && running.isDone());
            case "release" -> { held.rollback(); held.close(); held=null; running.get(10,TimeUnit.SECONDS); yield "rolled back blocker; purge completed"; }
            case "breakSalt" -> {
                DailySalt salt=ctx.getBean(DailySalt.class); salt.stamp("qa-prime");
                Field field=DailySalt.class.getDeclaredField("salt"); field.setAccessible(true);
                oldSalt=(byte[])field.get(salt); field.set(salt,new byte[0]); yield "actual empty keyed-hash key installed";
            }
            case "restoreSalt" -> {
                Field field=DailySalt.class.getDeclaredField("salt"); field.setAccessible(true);
                field.set(ctx.getBean(DailySalt.class),oldSalt); yield "key restored";
            }
            default -> throw new IllegalArgumentException(op);
        };
    }
    public static void main(String[] args) throws Exception {
        if (args.length>0 && args[0].equals("offline")) {
            List<String> statements=JSON.readValue(Files.readString(Path.of(args[2])),List.class);
            List<Object> output=new ArrayList<>();
            try(Connection c=DriverManager.getConnection(args[1],"sa","")) {
                for(String statement:statements) output.add(sql(c,statement));
            }
            Files.writeString(Path.of(args[3]),JSON.writeValueAsString(output)); return;
        }
        Path directory=Path.of(System.getProperty("qa.dir")); Files.createDirectories(directory);
        try(ConfigurableApplicationContext ctx=new SpringApplication(UrlshortApplication.class,QaConfig.class).run(args)) {
            Files.writeString(directory.resolve("ready"),JSON.writeValueAsString(Map.of("pid",ProcessHandle.current().pid(),"clock",CLOCK.instant().toString())));
            while(true) {
                Path request=directory.resolve("command.json");
                if(!Files.exists(request)) {Thread.sleep(10);continue;}
                Map<String,Object> cmd=JSON.readValue(Files.readString(request),Map.class); Files.delete(request);
                String op=(String)cmd.get("op"); Object result;
                try {result=op.equals("stop") ? "stopping" : command(cmd,ctx);}
                catch(Exception e) {result=Map.of("error",e.toString());}
                Path ack=directory.resolve("ack.tmp"); Files.writeString(ack,JSON.writeValueAsString(result));
                Files.move(ack,directory.resolve("ack.json"),StandardCopyOption.REPLACE_EXISTING);
                if(op.equals("stop")) break;
            }
        }
    }
}
