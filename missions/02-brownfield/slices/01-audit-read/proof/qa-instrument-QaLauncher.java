import java.nio.file.*;
import java.lang.reflect.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.config.BeanPostProcessor;
import tools.jackson.databind.json.JsonMapper;

public class QaLauncher {
    static final Path DIR=Path.of("/private/tmp/urlshort-audit-qa-35590f0");
    static final Path EVIDENCE=Path.of("/Users/andrzej/Documents/projekty/test/openrig/url-shortener/missions/02-brownfield/slices/01-audit-read/proof");
    static final JsonMapper JSON=JsonMapper.builder().build();
    static Connection held;
    public static void main(String[] args) throws Exception {
        if(args.length>0 && args[0].equals("dump")) {
            try(Connection c=DriverManager.getConnection(args[1],"sa","")){dump(c,args[2]);}
            return;
        }
        var context=new SpringApplication(dev.urlshort.UrlshortApplication.class,Instrumentation.class).run(args);
        DataSource ds=context.getBean(DataSource.class);
        while(context.isActive()) {
            Path command=DIR.resolve("command");
            if(Files.exists(command)) {
                String text=Files.readString(command).trim(); Files.delete(command);
                String[] parts=text.split(":",2); Map<String,Object> ack=new LinkedHashMap<>();
                try {
                    switch(parts[0]) {
                        case "reset":
                            if(held!=null){held.rollback();held.close();held=null;}
                            try(Connection c=ds.getConnection()) {
                                c.createStatement().executeUpdate("DELETE FROM audit_log");
                                for(int i=1;i<=Integer.parseInt(parts[1]);i++)
                                    insert(c,"qa-seeded-"+i,"qa-seed-request-"+i,OffsetDateTime.parse("2026-01-01T00:00:00Z").minusSeconds(i));
                            }
                            break;
                        case "hold":
                            held=ds.getConnection();held.setAutoCommit(false);
                            insert(held,"qa-held-row","qa-held-request",OffsetDateTime.now(ZoneOffset.UTC));
                            break;
                        case "commit":held.commit();held.close();held=null;break;
                        case "snapshot":try(Connection c=ds.getConnection()){dump(c,parts[1]);}break;
                        default:throw new IllegalArgumentException("unknown QA command");
                    }
                    ack.put("ok",true);
                } catch(Exception e){ack.put("ok",false);ack.put("type",e.getClass().getName());}
                Files.writeString(DIR.resolve("ack"),JSON.writeValueAsString(ack));
            }
            Thread.sleep(10);
        }
    }
    static void insert(Connection c,String id,String requestId,OffsetDateTime at) throws Exception {
        try(PreparedStatement p=c.prepareStatement("INSERT INTO audit_log (occurred_at,actor,action,entity,entity_id,request_id,after_state) VALUES (?,'anonymous','link.create','link',?,?,?)")) {
            p.setObject(1,at);p.setString(2,id);p.setString(3,requestId);
            p.setString(4,"{\"url\":\"https://example.org/qa-audit-seed?canary=QA-AUDIT-STORED-CANARY\",\"state\":\"active\"}");p.executeUpdate();
        }
    }
    static void dump(Connection c,String name) throws Exception {
        Map<String,Object> all=new LinkedHashMap<>();
        for(String table:List.of("audit_log","link","click")) {
            List<Map<String,Object>> rows=new ArrayList<>();
            try(ResultSet r=c.createStatement().executeQuery("SELECT * FROM "+table+" ORDER BY id")) {
                var m=r.getMetaData();while(r.next()) {
                    Map<String,Object> row=new LinkedHashMap<>();
                    for(int i=1;i<=m.getColumnCount();i++)row.put(m.getColumnLabel(i).toLowerCase(Locale.ROOT),r.getString(i));
                    rows.add(row);
                }
            }
            all.put(table,rows);
        }
        Files.writeString(EVIDENCE.resolve(name+".json"),JSON.writeValueAsString(all)+"\n");
    }
    static Object invoke(Object target,Method method,Object[] args) throws Throwable {
        try{return method.invoke(target,args);}catch(InvocationTargetException e){throw e.getCause();}
    }
    static Connection wrapped(Connection original) {
        return (Connection)Proxy.newProxyInstance(QaLauncher.class.getClassLoader(),new Class<?>[]{Connection.class},(p,m,a)->{
            if(m.getName().equals("prepareStatement") && a!=null && a[0] instanceof String sql
                    && sql.toUpperCase(Locale.ROOT).contains("FROM AUDIT_LOG") && Files.exists(DIR.resolve("fail-read")))
                throw new SQLException("QA-AUDIT-SQL-FAIL-CANARY https://example.org/never-log-this");
            return invoke(original,m,a);
        });
    }
    @Configuration(proxyBeanMethods=false)
    static class Instrumentation {
        @Bean static BeanPostProcessor qaDataSourceWrapper() {
            return new BeanPostProcessor() {
                public Object postProcessAfterInitialization(Object bean,String name) {
                    if(!name.equals("dataSource") || !(bean instanceof DataSource))return bean;
                    return Proxy.newProxyInstance(QaLauncher.class.getClassLoader(),new Class<?>[]{DataSource.class,AutoCloseable.class},(p,m,a)->{
                        Object result=invoke(bean,m,a);
                        return result instanceof Connection c?wrapped(c):result;
                    });
                }
            };
        }
        @Bean FilterRegistrationBean<Filter> qaPeer() {
            FilterRegistrationBean<Filter> r=new FilterRegistrationBean<>();
            r.setFilter((request,response,chain)->{
                HttpServletRequest http=(HttpServletRequest)request;String peer=http.getHeader("X-QA-Peer");
                if(peer==null)chain.doFilter(request,response);
                else chain.doFilter(new HttpServletRequestWrapper(http){public String getRemoteAddr(){return peer;}},response);
            });
            r.setOrder(Integer.MIN_VALUE+1);return r;
        }
    }
}
