// Disposable QA launcher. Candidate classes/resources are unchanged; this adds only
// the SPEC-authorized controlled Clock and a switchable real H2 DataSource.
// Source launch: JDK 21 java -cp '<candidate classes>:<resources>:<jar libs>/*' QaControl.java <Boot args>
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import javax.sql.DataSource;
import com.zaxxer.hikari.HikariDataSource;
import dev.urlshort.UrlshortApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

class QaControl {
    static final Path DIR = Path.of(System.getProperty("qa.control.dir"));
    static final MutableClock CLOCK = new MutableClock();
    static volatile boolean databaseDown;
    static final class MutableClock extends Clock {
        volatile Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        public Instant instant() { return now; }
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId ignored) { return this; }
    }
    static final class Gate extends HikariDataSource {
        @Override public Connection getConnection() throws SQLException {
            if (databaseDown) throw new SQLException("QA controlled database unavailable");
            return super.getConnection();
        }
    }
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DataSourceProperties.class)
    static class Controls {
        @Bean @Primary Clock qaClock() { return CLOCK; }
        @Bean FilterRegistrationBean<Filter> qaPeer() {
            FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
            registration.setFilter((request, response, chain) -> {
                HttpServletRequest http = (HttpServletRequest) request;
                String peer = http.getHeader("X-Qa-Peer");
                chain.doFilter(peer == null ? request : new HttpServletRequestWrapper(http) {
                    @Override public String getRemoteAddr() { return peer; }
                }, response);
            });
            return registration;
        }
        @Bean Gate dataSource(DataSourceProperties props) {
            Gate source = new Gate();
            source.setJdbcUrl(props.determineUrl());
            source.setUsername(props.determineUsername());
            source.setPassword(props.determinePassword());
            source.setPoolName("qa-pool");
            return source;
        }
    }
    static String cell(String value) {
        return value == null ? "" : "\"" + value.replace("\"", "\"\"") + "\"";
    }
    static void snapshot(DataSource source, String name) throws Exception {
        try (Connection conn = source.getConnection()) {
            for (String table : new String[]{"link", "audit_log", "click"}) {
                try (Statement stmt = conn.createStatement(); ResultSet rows = stmt.executeQuery("SELECT * FROM " + table + " ORDER BY id");
                     PrintWriter out = new PrintWriter(Files.newBufferedWriter(DIR.resolve(name + "-" + table + ".csv")))) {
                    ResultSetMetaData meta = rows.getMetaData();
                    for (int i = 1; i <= meta.getColumnCount(); i++) out.print((i > 1 ? "," : "") + meta.getColumnLabel(i));
                    out.println();
                    while (rows.next()) {
                        for (int i = 1; i <= meta.getColumnCount(); i++) out.print((i > 1 ? "," : "") + cell(rows.getString(i)));
                        out.println();
                    }
                }
            }
        }
    }
    public static void main(String[] args) throws Exception {
        var app = SpringApplication.run(new Class<?>[]{UrlshortApplication.class, Controls.class}, args);
        Files.writeString(DIR.resolve("ready"), CLOCK.instant().toString());
        try (BufferedReader input = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            int seq = 0;
            while ((line = input.readLine()) != null) {
                String[] cmd = line.split(" ", 2);
                switch (cmd[0]) {
                    case "SHIFT" -> CLOCK.now = CLOCK.now.plusMillis(Long.parseLong(cmd[1]));
                    case "DOWN" -> databaseDown = true;
                    case "UP" -> databaseDown = false;
                    case "SNAPSHOT" -> snapshot(app.getBean(DataSource.class), cmd[1]);
                    case "STOP" -> { app.close(); return; }
                    default -> throw new IllegalArgumentException("Unknown QA command");
                }
                Files.writeString(DIR.resolve("ack"), (++seq) + " " + line + " " + CLOCK.instant());
            }
        } finally { app.close(); }
    }
}
