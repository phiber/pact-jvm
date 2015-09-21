package au.com.dius.pact.provider.junit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;


@PactFile("/exampleSpec.json")
public class PactRuleTest {

    @Rule
    public PactRule pactRule = new PactRule();

    @Test
    @InteractionDescription(description = "test interaction", providerState = "test state")
    public void testState() throws IOException {
        // given

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new SuccessHandler());
        server.setExecutor(null); // creates a default executor
        server.start();

        // when
        PactConsumer.sendRequest("localhost", 8080);

        // then
        PactConsumer.verifyPact();
    }


    static class SuccessHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "All Done";
            t.sendResponseHeaders(201, response.length());
            try (OutputStream os = t.getResponseBody()) {
                os.write(response.getBytes());
                os.close();
            }
        }
    }


}
