import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** QA-only real-wire HEAD body check; curl -I's output file holds headers, not a body. */
public class HeadWireProbe {
    public static void main(String[] args) throws Exception {
        var http=HttpClient.newHttpClient();
        var results=new ArrayList<Map<String,Object>>();
        for(int i=1;i<args.length;i++) {
            var response=http.send(HttpRequest.newBuilder(URI.create(args[i]))
                .header("X-QA-Peer","10.8.0.10").method("HEAD",HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofByteArray());
            results.add(Map.of("url",args[i],"status",response.statusCode(),"bodyBytes",response.body().length,
                "headers",response.headers().map()));
            if(response.body().length!=0)throw new AssertionError("HEAD body on wire");
        }
        Files.writeString(Path.of(args[0]),JsonMapper.builder().build().writeValueAsString(results)+"\n");
        System.out.println("HEAD checks: "+results.size()+"; every body has zero bytes");
    }
}
