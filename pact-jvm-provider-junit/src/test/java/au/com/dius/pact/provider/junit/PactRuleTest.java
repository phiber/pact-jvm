package au.com.dius.pact.provider.junit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;


public class PactRuleTest {

    @Rule
    public PactConsumer pactConsumer = new PactConsumer("/exampleSpec.json");

    @Test
    @InteractionDescription(description = "test interaction", providerState = "test state")
    public void testState() throws IOException {
        // given
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new SuccessHandler());
        server.setExecutor(null); // creates a default executor
        server.start();

        // when
        pactConsumer.sendRequest("localhost", 8080);

        // then
        pactConsumer.verifyPactInteraction();
    }

    @Test
    @Ignore
    public void otherTest() {

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
