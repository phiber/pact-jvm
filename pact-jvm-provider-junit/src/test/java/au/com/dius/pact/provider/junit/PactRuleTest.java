package au.com.dius.pact.provider.junit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;


public class PactRuleTest {


    @Test
    public void testState() throws IOException {

        // given
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new SuccessHandler());
        server.setExecutor(null); // creates a default executor
        server.start();

        // when
        PactConsumer pactConsumer = PactConsumerBuilder
                .fromPactFile("/exampleSpec.json")
                .withInteraction("test interaction").build();

        pactConsumer.sendRequest("localhost", 8080);

        // then
        pactConsumer.verifyPactInteraction();
    }



    static class SuccessHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "All Done";
            t.sendResponseHeaders(200, response.length());
            try (OutputStream os = t.getResponseBody()) {
                os.write(response.getBytes());
                os.close();
            }
        }
    }


}
