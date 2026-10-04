package dev.urlshort.click;

import dev.urlshort.UrlshortApplication;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Independent, disposable integrated HTTP/storage controls. No product/test source edits. */
public class ReviewWaveProbe {
    static final Path OUT = Path.of("docs/review/03-ambiguous-analytics/proof/wave-effects-94aa2c0.json");
    static final List<Object> observations = new ArrayList<>();
    static final List<String> checks = new ArrayList<>();
    static final String TARGET = "https://example.com/wave-review";
    static String base;
    static JsonMapper json;
    static volatile Instant now = Instant.parse("2030-01-01T12:00:00Z");
    @Configuration(proxyBeanMethods=false)
    static class ClockControl {
        @Bean @Primary Clock reviewClock() {
            return new Clock() {
                public ZoneId getZone() { return ZoneOffset.UTC; }
                public Clock withZone(ZoneId z) { return this; }
                public Instant instant() { return now; }
            };
        }
    }
    static void require(boolean ok, String claim) {
        if (!ok) throw new AssertionError(claim);
        checks.add(claim);
    }
    record Reply(int status, Map<String,String> headers, String body) {}
    static Reply http(String method, String path, String body, String... headers) throws Exception {
        List<String> cmd = new ArrayList<>(List.of("scripts/http","-sS","-i","--max-time","10","-X",method));
        for (String h:headers) cmd.addAll(List.of("-H",h));
        if (body != null) cmd.addAll(List.of("-H","Content-Type: application/json","--data",body));
        cmd.add(base+path);
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        String raw = new String(p.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        if (p.waitFor()!=0) throw new AssertionError(raw);
        String[] parts=raw.split("\r?\n\r?\n",2);
        String[] lines=parts[0].split("\r?\n");
        Map<String,String> hs=new TreeMap<>();
        for (int i=1;i<lines.length;i++) {
            int colon=lines[i].indexOf(':');
            if(colon>0) hs.put(lines[i].substring(0,colon).toLowerCase(Locale.ROOT),lines[i].substring(colon+1).trim());
        }
        Reply r=new Reply(Integer.parseInt(lines[0].split(" ")[1]),hs,parts.length==2?parts[1]:"");
        observations.add(Map.of("method",method,"path",path,"raw",raw,"status",r.status(),"headers",hs,"body",r.body()));
        require(hs.containsKey("x-request-id"),"server request id on "+method+" "+path);
        return r;
    }
    static String create() throws Exception {
        Reply r=http("POST","/api/links","{\"url\":\""+TARGET+"\"}");
        require(r.status()==201,"link create 201");
        return json.readTree(r.body()).get("code").asString();
    }
    static JsonNode stats(String code) throws Exception {
        Reply r=http("GET","/api/links/"+code+"/stats",null);
        require(r.status()==200,"statistics 200");
        JsonNode n=json.readTree(r.body());
        require(n.propertyNames().equals(Set.of("code","totalClicks","clicksPerDay","topReferrers")),"exact four top-level fields");
        return n;
    }
    static void open(String code,String forwarded,boolean bot) throws Exception {
        Reply r=http("GET","/"+code,null,"X-Forwarded-For: "+forwarded,
            "User-Agent: "+(bot?"wave-crawler-PRIVATE":"Mozilla/5.0 wave-PRIVATE"),
            "Referer: https://news.example/private-path?private-query=1");
        require(r.status()==302 && TARGET.equals(r.headers().get("location")) && "no-store".equals(r.headers().get("cache-control")),"stored redirect and no-store");
    }
    static double meter(String name) throws Exception {
        Reply r=http("GET","/actuator/metrics/"+name,null);
        require(r.status()==200,"metric 200 "+name);
        return json.readTree(r.body()).get("measurements").get(0).get("value").asDouble();
    }
    static void run(boolean trusted) throws Exception {
        now=Instant.parse("2030-01-01T12:00:00Z");
        Path db=Files.createTempDirectory("urlshort-wave-review-").resolve("db");
        try(var context=new SpringApplicationBuilder(UrlshortApplication.class,ClockControl.class).run(
            "--server.address=127.0.0.1","--server.port=0","--spring.datasource.url=jdbc:h2:file:"+db+";DB_CLOSE_ON_EXIT=FALSE",
            "--urlshort.click.purge-enabled=false","--urlshort.rate-limit.trusted-proxies="+(trusted?"127.0.0.1":""))) {
            base="http://127.0.0.1:"+((WebServerApplicationContext)context).getWebServer().getPort();
            json=context.getBean(JsonMapper.class);
            JdbcClient jdbc=context.getBean(JdbcClient.class);
            ClickRecorder recorder=context.getBean(ClickRecorder.class);
            String code=create();
            require(stats(code).get("totalClicks").asLong()==0,"empty link");
            open(code,"203.0.113.7",false);
            open(code,"203.0.113.7",true);
            open(code,"198.51.100.90, 203.0.113.8",false);
            open(code,"203.0.113.9",false);
            recorder.settle();
            JsonNode n=stats(code), d=n.get("clicksPerDay").get(0);
            require(n.get("totalClicks").asLong()==4 && d.get("clicks").asLong()==4 && d.get("botClicks").asLong()==1 && d.get("uniqueVisitors").asLong()==(trusted?3:1),"trusted="+trusted+": 4 clicks, 1 bot and correct unique count");
            require(d.propertyNames().equals(Set.of("date","clicks","uniqueVisitors","botClicks")),"exact four daily fields");
            List<Map<String,Object>> rows=jdbc.sql("SELECT * FROM click ORDER BY id").query().listOfRows();
            require(rows.size()==4 && rows.stream().allMatch(r->r.get("CREATED_AT").equals(r.get("UPDATED_AT")) && "anonymous".equals(r.get("CREATED_BY")) && "anonymous".equals(r.get("UPDATED_BY"))),"V3 click audit columns remain populated");
            String stored=rows.toString();
            for(String privateValue:List.of("203.0.113.","198.51.100.","wave-PRIVATE","private-path","private-query")) {
                require(!stored.contains(privateValue) && !n.toString().contains(privateValue),"no private value in rows/response: "+privateValue);
            }
            for(Map<String,Object> row:rows) require(!n.toString().contains(row.get("CLIENT_HASH").toString()),"hash absent from response");
            double count=meter("urlshort.clicks.recorded");
            require(count==4,"recorded counts stored rows");
            Reply denied=http("GET","/api/audit",null,"X-Forwarded-For: 127.0.0.1");
            require(denied.status()==403 && denied.headers().get("content-type").startsWith("application/problem+json"),"shared identity does not open audit guard");
            require(http("GET","/api/audit",null).status()==200,"direct loopback audit read remains usable");
            now=now.plus(Duration.ofDays(1));
            open(code,"203.0.113.7",false); recorder.settle();
            n=stats(code);
            require(n.get("totalClicks").asLong()==5 && n.get("clicksPerDay").size()==2 && n.get("clicksPerDay").get(1).get("uniqueVisitors").asLong()==1,"next UTC day has its own count");
            String failed=create();
            long failedId=jdbc.sql("SELECT id FROM link WHERE code=:code").param("code",failed).query(Long.class).single();
            jdbc.sql("ALTER TABLE click ADD CONSTRAINT review_reject CHECK (link_id <> "+failedId+")").update();
            open(failed,"203.0.113.8",false); recorder.settle();
            require(stats(failed).get("totalClicks").asLong()==0 && meter("urlshort.clicks.recorded")==5 && meter("urlshort.clicks.lost?tag=reason:write%20failed")==1,"actual JDBC failure remains 302 and counts only lost");
            Reply scrape=http("GET","/actuator/prometheus",null);
            List<String> series=scrape.body().lines().filter(s->s.startsWith("urlshort_clicks_")).toList();
            require(series.size()==6 && series.stream().allMatch(s->s.matches("urlshort_clicks_recorded_total 5\\.0") || s.matches("urlshort_clicks_lost_total\\{reason=\"(rejected|reduction failed|write failed|shutdown deadline|shutdown deadline, outcome unknown)\"} [01]\\.0")),"only static click counter series");
            var links=jdbc.sql("SELECT * FROM link ORDER BY id").query().listOfRows();
            var audit=jdbc.sql("SELECT * FROM audit_log ORDER BY id").query().listOfRows();
            require(links.stream().allMatch(r->r.get("CREATED_AT").equals(r.get("UPDATED_AT"))),"V4 create stamps unchanged by analytics");
            now=now.plus(Duration.ofDays(91)); context.getBean(ClickPurge.class).runNow();
            require(stats(code).get("totalClicks").asLong()==0 && stats(code).get("clicksPerDay").isEmpty(),"retention removes old raw and unique figures together");
            require(links.equals(jdbc.sql("SELECT * FROM link ORDER BY id").query().listOfRows()) && audit.equals(jdbc.sql("SELECT * FROM audit_log ORDER BY id").query().listOfRows()),"purge preserves every link/audit field");
            require(meter("urlshort.clicks.recorded")==5,"purge does not decrement cumulative meter");
        }
    }
    public static void main(String[] args) throws Exception {
        try { run(false); run(true); }
        finally {
            if(json!=null) Files.writeString(OUT,json.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("checks",checks,"observations",observations))+"\n");
        }
        System.out.println("WAVE_CONTROLS_PASS checks="+checks.size()+" requests="+observations.size());
    }
}
