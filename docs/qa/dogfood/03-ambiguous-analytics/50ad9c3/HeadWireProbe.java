import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Instant;
public class HeadWireProbe {
 public static void main(String[] args) throws Exception {
  HttpClient client=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
  for(int i=0;i<args.length;i+=2) {
   String before=Instant.now().toString();
   var r=client.send(HttpRequest.newBuilder(URI.create(args[i+1])).method("HEAD",HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofByteArray());
   String out="{\"name\":\""+args[i]+"\",\"url\":\""+args[i+1]+"\",\"method\":\"HEAD\",\"before\":\""+before+"\",\"after\":\""+Instant.now()+"\",\"status\":"+r.statusCode()+",\"bodyBytes\":"+r.body().length+",\"requestId\":\""+r.headers().firstValue("X-Request-Id").orElse("")+"\",\"location\":\""+r.headers().firstValue("Location").orElse("")+"\"}";
   Files.writeString(Path.of("docs/qa/dogfood/03-ambiguous-analytics/50ad9c3/http/"+args[i]+".wire.json"),out+"\n");
   System.out.println(out);
  }
 }
}
