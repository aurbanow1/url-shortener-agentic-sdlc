import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;

/** Independent V4 controls; temporary file databases, exact design migrations, literal rollback. */
public class ReviewLinkMigrationProbe {
    static final Path V3 = Path.of("missions/02-brownfield/slices/02-click-retention/design-probe/migration");
    static final Path V4 = Path.of("missions/02-brownfield/slices/04-audit-columns/design-probe/migration/V4__add_link_audit_columns.sql");
    static final String OLD_LINKS = "SELECT id,code,url,created_at,retired_at,idempotency_key FROM link ORDER BY id";
    static final String OLD_AUDIT = "SELECT id,occurred_at,actor,action,entity,entity_id,request_id,before_state,after_state FROM audit_log ORDER BY id";
    static int assertions;

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("review-link-v4-");
        String url = url(root, "upgrade");
        migrate(url, "3");
        try (Connection c = DriverManager.getConnection(url, "sa", "")) {
            execute(c, "INSERT INTO link(code,url,created_at,retired_at,idempotency_key) VALUES ('active01','https://example.com/',TIMESTAMP WITH TIME ZONE '2001-01-01 00:00:00Z',NULL,'key01'),('retired1','https://example.com/',TIMESTAMP WITH TIME ZONE '2001-01-01 00:00:00Z',TIMESTAMP WITH TIME ZONE '2001-02-01 00:00:00Z',NULL),('release1','https://example.com/',TIMESTAMP WITH TIME ZONE '2001-01-01 00:00:00Z',NULL,NULL)");
            audit(c, "2001-01-01T00:00:00Z", "anonymous");
            audit(c, Instant.now().plusSeconds(86400).toString(), "s".repeat(64));
            click(c);
            String beforeColumns = columns(c, true), beforeKeys = keys(c);
            String beforeLinks = rows(c, OLD_LINKS), beforeAudit = rows(c, OLD_AUDIT);
            Instant before = Instant.now();
            migrate(url, null);
            Instant after = Instant.now();
            equal(beforeColumns, columns(c, true), "original column types/nullability/defaults preserved");
            equal(beforeKeys, keys(c), "keys, checks and click foreign-key columns preserved");
            equal(beforeLinks, rows(c, OLD_LINKS), "all original link values preserved");
            equal(beforeAudit, rows(c, OLD_AUDIT), "all original audit values preserved");
            equal("[3]", rows(c, "SELECT COUNT(*) FROM link WHERE updated_at=COALESCE(retired_at,created_at) AND created_by='anonymous' AND updated_by='anonymous'"), "active/retired/released backfill");
            equal("[2]", rows(c, "SELECT COUNT(*) FROM audit_log WHERE created_at=updated_at AND created_by=actor AND updated_by=actor"), "audit times and actors backfilled including width 64");
            try (var s=c.createStatement(); var rs=s.executeQuery("SELECT created_at FROM audit_log WHERE id=2")) {
                rs.next(); Instant cap=rs.getObject(1,OffsetDateTime.class).toInstant();
                equal(true,!cap.isBefore(before) && !cap.isAfter(after),"future event capped to migration interval");
            }
            schema(c);
            // Flyway's connection is closed; this keeper was open across all ALTER statements.
            execute(c,"INSERT INTO link(code,url,created_at) VALUES ('newlink1','https://example.com/',TIMESTAMP WITH TIME ZONE '2002-01-01 00:00:00Z')");
            equal("[true|anonymous|anonymous]",rows(c,"SELECT updated_at IS NOT NULL,created_by,updated_by FROM link WHERE code='newlink1'"),"legacy insert fills omitted columns after DDL connection retirement");
            execute(c,"UPDATE link SET updated_at=TIMESTAMP WITH TIME ZONE '2002-01-01 00:00:00Z',updated_by='anonymous' WHERE code='newlink1'");
            equal("[true]",rows(c,"SELECT created_at=updated_at FROM link WHERE code='newlink1'"),"post-insert stamp uses domain clock");
            audit(c,"2002-01-01T00:00:00Z","anonymous");
            equal("[true|true|anonymous|anonymous]",rows(c,"SELECT created_at=updated_at,created_at<>occurred_at,created_by,updated_by FROM audit_log WHERE id=3"),"unchanged audit insert uses equal database-clock defaults");
            click(c);
            equal("[2]",rows(c,"SELECT COUNT(*) FROM click"),"click FK still accepts existing parent after link ALTER");
            rejected(c,"INSERT INTO click(link_id,clicked_at,clicked_on,user_agent_class,client_hash) VALUES(999,CURRENT_TIMESTAMP,CURRENT_DATE,'browser','"+"a".repeat(64)+"')","23506");
            rejected(c,"INSERT INTO link(code,url,created_at) VALUES('active01','https://example.com/',CURRENT_TIMESTAMP)","23505");
            rejected(c,"INSERT INTO link(code,url,created_at) VALUES('x','https://example.com/',CURRENT_TIMESTAMP)","23513");
            String untouched=rows(c,"SELECT * FROM link ORDER BY id");
            c.setAutoCommit(false);
            execute(c,"UPDATE link SET retired_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP,updated_by='anonymous' WHERE id=1 AND retired_at IS NULL");
            rejected(c,"INSERT INTO audit_log(occurred_at,actor,action,entity,entity_id,request_id,after_state) VALUES(CURRENT_TIMESTAMP,'anonymous','link.retire','link','active01',NULL,'{}')","23502");
            c.rollback(); c.setAutoCommit(true);
            equal(untouched,rows(c,"SELECT * FROM link ORDER BY id"),"failed audit transaction rolls back stamps and retirement");
            equal(1,execute(c,"UPDATE link SET retired_at=TIMESTAMP WITH TIME ZONE '2003-01-01 00:00:00Z',updated_at=TIMESTAMP WITH TIME ZONE '2003-01-01 00:00:00Z',updated_by='anonymous' WHERE id=1 AND retired_at IS NULL"),"first retire updates one row");
            String retired=rows(c,"SELECT * FROM link WHERE id=1");
            equal(0,execute(c,"UPDATE link SET retired_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP,updated_by='anonymous' WHERE id=1 AND retired_at IS NULL"),"second retire updates nothing");
            equal(retired,rows(c,"SELECT * FROM link WHERE id=1"),"second retire preserves stamps");
            c.setAutoCommit(false);
            execute(c,"UPDATE link SET idempotency_key=NULL WHERE id=1");
            execute(c,"UPDATE link SET updated_at=TIMESTAMP WITH TIME ZONE '2004-01-01 00:00:00Z',updated_by='anonymous' WHERE id=1");
            c.commit(); c.setAutoCommit(true);
            equal("[true|true|true]",rows(c,"SELECT idempotency_key IS NULL,updated_at=TIMESTAMP WITH TIME ZONE '2004-01-01 00:00:00Z',retired_at=TIMESTAMP WITH TIME ZONE '2003-01-01 00:00:00Z' FROM link WHERE id=1"),"release stamp preserves retirement");
            String rollbackLinks=rows(c,OLD_LINKS), rollbackAudit=rows(c,OLD_AUDIT);
            var statements=Files.readAllLines(V4).stream().filter(s -> s.startsWith("--   ALTER") || s.startsWith("--   DELETE")).toList();
            equal(5,statements.size(),"literal rollback header selected");
            for(String line:statements) for(String sql:line.substring(5).split(";")) if(!sql.isBlank()) execute(c,sql);
            equal(beforeColumns,columns(c,false),"rollback restores V3 column schema");
            equal(beforeKeys,keys(c),"rollback preserves keys and click FK");
            equal(rollbackLinks,rows(c,OLD_LINKS),"rollback preserves original link values");
            equal(rollbackAudit,rows(c,OLD_AUDIT),"rollback preserves original audit values");
            equal("[1, 2, 3]",rows(c,"SELECT \"version\" FROM \"flyway_schema_history\" WHERE \"version\" IS NOT NULL ORDER BY \"installed_rank\""),"rollback removes only V4 history");
            migrate(url,null);
            equal("[4]",rows(c,"SELECT COUNT(*) FROM link WHERE updated_at=COALESCE(retired_at,created_at)"),"V4 reapplies");
        }
        try(Connection c=DriverManager.getConnection(url,"sa","")) { click(c); equal("[3]",rows(c,"SELECT COUNT(*) FROM click"),"click FK after reopen and rollback/reapply"); }
        String fresh=url(root,"fresh"); migrate(fresh,null);
        try(Connection c=DriverManager.getConnection(fresh,"sa","")) { schema(c); }
        System.out.println("PASS: "+assertions+" assertions; exact V4 and rollback; temporary file databases: "+root);
    }
    static String url(Path root,String name) { return "jdbc:h2:file:"+root.resolve(name)+";MODE=PostgreSQL;DB_CLOSE_ON_EXIT=FALSE"; }
    static void migrate(String url,String target) { var c=Flyway.configure().dataSource(url,"sa","").locations("classpath:db/migration","filesystem:"+V3.toAbsolutePath(),"filesystem:"+V4.getParent().toAbsolutePath()); if(target!=null)c.target(target); c.load().migrate(); }
    static void audit(Connection c,String at,String actor) throws Exception { try(var p=c.prepareStatement("INSERT INTO audit_log(occurred_at,actor,action,entity,entity_id,request_id,after_state) VALUES(?,?,'link.create','link','active01','review-request','{}')")) {p.setObject(1,OffsetDateTime.parse(at));p.setString(2,actor);p.executeUpdate();} }
    static void click(Connection c) throws Exception { execute(c,"INSERT INTO click(link_id,clicked_at,clicked_on,user_agent_class,client_hash) VALUES(1,CURRENT_TIMESTAMP,CURRENT_DATE,'browser','"+"a".repeat(64)+"')"); }
    static void schema(Connection c) throws Exception {
        equal("[3]",rows(c,"SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE (TABLE_NAME='LINK' AND COLUMN_NAME='UPDATED_AT' OR TABLE_NAME='AUDIT_LOG' AND COLUMN_NAME IN ('CREATED_AT','UPDATED_AT')) AND DATA_TYPE='TIMESTAMP WITH TIME ZONE' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT='CURRENT_TIMESTAMP'"),"three new non-null timestamp defaults");
        equal("[4]",rows(c,"SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME IN ('LINK','AUDIT_LOG') AND COLUMN_NAME IN ('CREATED_BY','UPDATED_BY') AND IS_NULLABLE='NO' AND CHARACTER_MAXIMUM_LENGTH=64 AND COLUMN_DEFAULT='''anonymous'''"),"four non-null static actor defaults");
    }
    static String columns(Connection c,boolean oldOnly) throws Exception { return rows(c,"SELECT TABLE_NAME,COLUMN_NAME,DATA_TYPE,CHARACTER_MAXIMUM_LENGTH,IS_NULLABLE,COLUMN_DEFAULT,IS_IDENTITY FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME IN ('LINK','AUDIT_LOG')"+(oldOnly?" AND COLUMN_NAME NOT IN ('UPDATED_AT','CREATED_BY','UPDATED_BY') AND NOT(TABLE_NAME='AUDIT_LOG' AND COLUMN_NAME='CREATED_AT')":"")+" ORDER BY TABLE_NAME,ORDINAL_POSITION"); }
    static String keys(Connection c) throws Exception { return rows(c,"SELECT TABLE_NAME,CONSTRAINT_NAME,CONSTRAINT_TYPE FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_NAME IN ('LINK','AUDIT_LOG','CLICK') AND CONSTRAINT_TYPE<>'PRIMARY KEY' ORDER BY 1,2")+rows(c,"SELECT TABLE_NAME,COLUMN_NAME,ORDINAL_POSITION FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE TABLE_NAME IN ('LINK','AUDIT_LOG','CLICK') ORDER BY 1,2,3"); }
    static int execute(Connection c,String sql) throws SQLException { try(var s=c.createStatement()) {return s.executeUpdate(sql);} }
    static void rejected(Connection c,String sql,String state) throws Exception { try {execute(c,sql);throw new AssertionError("Accepted forbidden input");} catch(SQLException ex) {equal(state,ex.getSQLState(),"constraint rejection "+state);} }
    static String rows(Connection c,String sql) throws SQLException {List<String> result=new ArrayList<>();try(var s=c.createStatement();var rs=s.executeQuery(sql)){while(rs.next()){List<String> row=new ArrayList<>();for(int i=1;i<=rs.getMetaData().getColumnCount();i++)row.add(String.valueOf(rs.getObject(i)));result.add(String.join("|",row));}}return result.toString();}
    static void equal(Object expected,Object actual,String label) {if(!expected.equals(actual))throw new AssertionError(label+": expected "+expected+", actual "+actual);assertions++;System.out.println("PASS "+label);}
}
