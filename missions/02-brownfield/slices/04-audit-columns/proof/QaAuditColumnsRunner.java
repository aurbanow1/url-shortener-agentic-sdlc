package dev.urlshort.click;

// External QA fixture: product classes and migrations are unchanged. No HTTP controls added.
import dev.urlshort.UrlshortApplication;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;
import tools.jackson.databind.json.JsonMapper;

public class QaAuditColumnsRunner {
    static final JsonMapper JSON = JsonMapper.builder().build();
    static final MovingClock CLOCK = new MovingClock();
    static class MovingClock extends Clock {
        volatile Instant now = Instant.parse("2026-08-01T12:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }
    @Configuration(proxyBeanMethods = false)
    static class QaConfig { @Bean @Primary Clock qaClock() { return CLOCK; } }
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
            Files.writeString(directory.resolve("ready"),"ready");
            while(true) {
                Path request=directory.resolve("command.json");
                if(!Files.exists(request)) {Thread.sleep(10);continue;}
                Map<String,Object> cmd=JSON.readValue(Files.readString(request),Map.class); Files.delete(request);
                String op=(String)cmd.get("op"); Object result;
                try {
                    result = switch(op) {
                        case "clock" -> { CLOCK.now=Instant.parse((String)cmd.get("instant")); yield CLOCK.now.toString(); }
                        case "sql" -> { try(Connection c=ctx.getBean(DataSource.class).getConnection()) { yield sql(c,(String)cmd.get("sql")); } }
                        case "settle" -> { ctx.getBean(ClickRecorder.class).settle(); yield "settled"; }
                        case "stop" -> "stopping";
                        default -> throw new IllegalArgumentException(op);
                    };
                } catch(Exception e) { result=Map.of("error",e.toString()); }
                Path ack=directory.resolve("ack.tmp"); Files.writeString(ack,JSON.writeValueAsString(result));
                Files.move(ack,directory.resolve("ack.json"),StandardCopyOption.REPLACE_EXISTING);
                if(op.equals("stop")) break;
            }
        }
    }
}
