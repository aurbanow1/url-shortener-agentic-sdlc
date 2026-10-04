package dev.urlshort.click;

// External QA controls only: unchanged product code, real Tomcat/JDBC, no HTTP control endpoint.
import dev.urlshort.UrlshortApplication;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.json.JsonMapper;

public class QaIdentityRunner {
    static final JsonMapper JSON = JsonMapper.builder().build();
    static volatile String peer = "127.0.0.1";
    static volatile Instant instant = Instant.parse("2026-10-01T12:00:00Z");
    static volatile CountDownLatch hold;
    static volatile boolean held;
    static volatile Map<String,String> headerOverrides=Map.of();
    static class FixedClock extends Clock {
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return instant; }
    }
    static class HeldStore extends ClickStore {
        HeldStore(JdbcClient jdbc) { super(jdbc); }
        @Override void insert(Click click) {
            CountDownLatch gate=hold;
            if(gate!=null) {
                held=true;
                try { if(!gate.await(15,TimeUnit.SECONDS)) throw new IllegalStateException("QA hold deadline"); }
                catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
            }
            super.insert(click);
        }
    }
    @Configuration(proxyBeanMethods=false)
    static class Controls {
        @Bean @Primary Clock qaClock() { return new FixedClock(); }
        @Bean @Primary ClickStore qaStore(DataSource ds) { return new HeldStore(JdbcClient.create(ds)); }
        @Bean FilterRegistrationBean<jakarta.servlet.Filter> qaPeer() {
            FilterRegistrationBean<jakarta.servlet.Filter> filter=new FilterRegistrationBean<>();
            filter.setOrder(Ordered.HIGHEST_PRECEDENCE+1);
            filter.setFilter((request,response,chain)->chain.doFilter(new HttpServletRequestWrapper((HttpServletRequest)request) {
                @Override public String getRemoteAddr() { return peer; }
                @Override public String getHeader(String name) {
                    String key=name.toLowerCase(Locale.ROOT);
                    return headerOverrides.containsKey(key) ? headerOverrides.get(key) : super.getHeader(name);
                }
            },response));
            return filter;
        }
    }
    static Object sql(Connection connection,String text) throws Exception {
        try(Statement statement=connection.createStatement()) {
            if(!statement.execute(text)) return Map.of("updated",statement.getUpdateCount());
            try(ResultSet rs=statement.getResultSet()) {
                List<Map<String,Object>> rows=new ArrayList<>(); ResultSetMetaData md=rs.getMetaData();
                while(rs.next()) {
                    Map<String,Object> row=new LinkedHashMap<>();
                    for(int i=1;i<=md.getColumnCount();i++) row.put(md.getColumnLabel(i).toLowerCase(Locale.ROOT),rs.getString(i));
                    rows.add(row);
                }
                return rows;
            }
        }
    }
    public static void main(String[] args) throws Exception {
        Path directory=Path.of(System.getProperty("qa.dir")); Files.createDirectories(directory);
        try(ConfigurableApplicationContext ctx=new SpringApplication(UrlshortApplication.class,Controls.class).run(args)) {
            Files.writeString(directory.resolve("ready"),"ready");
            while(true) {
                Path request=directory.resolve("command.json");
                if(!Files.exists(request)) { Thread.sleep(5); continue; }
                Map<String,Object> command=JSON.readValue(Files.readString(request),Map.class); Files.delete(request);
                String op=(String)command.get("op"); Object result;
                try {
                    result=switch(op) {
                        case "peer" -> { peer=(String)command.get("peer"); yield peer; }
                        case "headers" -> { headerOverrides=Map.copyOf((Map<String,String>)command.get("headers")); yield headerOverrides; }
                        case "clock" -> { instant=Instant.parse((String)command.get("instant")); yield instant.toString(); }
                        case "sql" -> { try(Connection c=ctx.getBean(DataSource.class).getConnection()) { yield sql(c,(String)command.get("sql")); } }
                        case "settle" -> { ctx.getBean(ClickRecorder.class).settle(); yield "settled"; }
                        case "hold" -> { held=false; hold=new CountDownLatch(1); yield "holding"; }
                        case "held" -> held;
                        case "release" -> { if(hold!=null) hold.countDown(); hold=null; yield "released"; }
                        case "stop" -> { if(hold!=null) hold.countDown(); yield "stopping"; }
                        default -> throw new IllegalArgumentException(op);
                    };
                } catch(Exception e) { result=Map.of("error",e.toString()); }
                Path temporary=directory.resolve("ack.tmp"); Files.writeString(temporary,JSON.writeValueAsString(result));
                Files.move(temporary,directory.resolve("ack.json"),StandardCopyOption.REPLACE_EXISTING);
                if(op.equals("stop")) break;
            }
        }
    }
}
