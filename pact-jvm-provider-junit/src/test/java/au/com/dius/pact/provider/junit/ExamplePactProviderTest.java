package au.com.dius.pact.provider.junit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;


@RunWith(PactRunner.class)
@PactFile("/exampleSpec.json")
public class ExamplePactProviderTest {

    public ExamplePactProviderTest() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new SuccessHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    @ProviderState("test state")
    public void testState() throws IOException {

    }

    @ProviderState("a user named Mary exists")
    public void withAUserNamedMary() {
    }

    static class SuccessHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "All Done";
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

}
