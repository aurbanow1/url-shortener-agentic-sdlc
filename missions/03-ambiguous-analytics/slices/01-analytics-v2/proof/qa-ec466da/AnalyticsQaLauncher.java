package qa;

import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.h2.api.Trigger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** QA-only clock, peer and physical H2 insert controls; candidate classes are untouched. */
public class AnalyticsQaLauncher {
    static final JsonMapper JSON=JsonMapper.builder().build();
    static final MovingClock CLOCK=new MovingClock();
    static volatile boolean fail;
    static volatile int delayMs;
    static Path directory;
    static Path evidence;

    public static void main(String[] args) throws Exception {
        if(args.length>0 && args[0].equals("dump")) {
            try(Connection c=DriverManager.getConnection(args[1],"sa","")) {
                Files.writeString(Path.of(args[2]),JSON.writeValueAsString(snapshot(c))+"\n");
            }
            return;
        }
        directory=Path.of(System.getProperty("qa.dir"));
        evidence=Path.of(System.getProperty("qa.evidence"));
        Files.createDirectories(directory);
        var context=new SpringApplication(dev.urlshort.UrlshortApplication.class,Instrumentation.class).run(args);
        DataSource ds=context.getBean(DataSource.class);
        try(Connection c=ds.getConnection()) {
            String table=tableName(c,"click");
            c.createStatement().execute("CREATE TRIGGER qa_click_insert BEFORE INSERT ON "+quoted(table)
                +" FOR EACH ROW CALL 'qa.AnalyticsQaLauncher$InsertControl'");
        }
        Files.writeString(directory.resolve("ready"),"ready\n");
        while(context.isActive()) {
            Path command=directory.resolve("command.json");
            if(Files.exists(command)) {
                JsonNode n=JSON.readTree(Files.readString(command));Files.delete(command);
                Map<String,Object> ack=new LinkedHashMap<>();
                ack.put("id",n.get("id").asString());
                try {
                    switch(n.get("action").asString()) {
                        case "clock":CLOCK.at=Instant.parse(n.get("instant").asString());break;
                        case "mode":fail=n.get("fail").asBoolean();delayMs=n.get("delayMs").asInt();break;
                        case "settle":
                            var method=dev.urlshort.click.ClickRecorder.class.getDeclaredMethod("settle");
                            method.setAccessible(true);method.invoke(context.getBean(dev.urlshort.click.ClickRecorder.class));
                            break;
                        case "snapshot":
                            try(Connection c=ds.getConnection()) {
                                Files.writeString(evidence.resolve(n.get("name").asString()+".json"),
                                    JSON.writeValueAsString(snapshot(c))+"\n");
                            }
                            break;
                        default:throw new IllegalArgumentException("unknown QA control");
                    }
                    ack.put("ok",true);ack.put("action",n.get("action").asString());ack.put("instant",CLOCK.instant().toString());
                    ack.put("fail",fail);ack.put("delayMs",delayMs);
                }catch(Exception ex) {ack.put("ok",false);ack.put("type",ex.getClass().getName());}
                Files.writeString(directory.resolve("ack.json"),JSON.writeValueAsString(ack)+"\n");
            }
            Thread.sleep(10);
        }
    }
    static String quoted(String name) {return "\""+name.replace("\"","\"\"")+"\"";}
    static String tableName(Connection c,String wanted) throws SQLException {
        try(ResultSet r=c.getMetaData().getTables(null,null,"%",new String[]{"TABLE"})) {
            while(r.next())if(r.getString("TABLE_NAME").equalsIgnoreCase(wanted))return r.getString("TABLE_NAME");
        }
        throw new SQLException("missing QA snapshot table "+wanted);
    }
    static Map<String,Object> snapshot(Connection c) throws SQLException {
        Map<String,Object> all=new LinkedHashMap<>();
        for(String wanted:List.of("link","click","audit_log","user_agent_class")) {
            List<Map<String,Object>> rows=new ArrayList<>();
            try(ResultSet r=c.createStatement().executeQuery("SELECT * FROM "+quoted(tableName(c,wanted)))) {
                var metadata=r.getMetaData();
                while(r.next()) {
                    Map<String,Object> row=new LinkedHashMap<>();
                    for(int i=1;i<=metadata.getColumnCount();i++)row.put(metadata.getColumnLabel(i).toLowerCase(Locale.ROOT),r.getString(i));
                    rows.add(row);
                }
            }
            all.put(wanted,rows);
        }
        return all;
    }
    public static class InsertControl implements Trigger {
        public void fire(Connection c,Object[] oldRow,Object[] newRow) throws SQLException {
            if(delayMs>0) {
                try {Thread.sleep(delayMs);}catch(InterruptedException ex) {
                    Thread.currentThread().interrupt();throw new SQLException("QA interrupted delay",ex);
                }
            }
            if(fail)throw new SQLException("QA-ANALYTICS-WRITE-CANARY https://must-not-be-logged.example/private");
        }
    }
    static class MovingClock extends Clock {
        volatile Instant at=Instant.parse("2026-10-01T12:00:00Z");
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zone){return Clock.fixed(instant(),zone);}
        public Instant instant(){return at;}
    }
    @Configuration(proxyBeanMethods=false)
    static class Instrumentation {
        @Bean @Primary Clock qaClock(){return CLOCK;}
        @Bean FilterRegistrationBean<Filter> qaPeer() {
            FilterRegistrationBean<Filter> r=new FilterRegistrationBean<>();
            r.setFilter((request,response,chain)->{
                HttpServletRequest http=(HttpServletRequest)request;
                String peer=http.getHeader("X-QA-Peer");
                if(peer==null)chain.doFilter(request,response);
                else chain.doFilter(new HttpServletRequestWrapper(http){public String getRemoteAddr(){return peer;}},response);
            });
            r.setOrder(Integer.MIN_VALUE+1);return r;
        }
    }
}
