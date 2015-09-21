package au.com.dius.pact.provider.junit;

import au.com.dius.pact.model.*;
import au.com.dius.pact.model.dispatch.HttpClient;
import scala.Option;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext$;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class PactConsumer {

    private static au.com.dius.pact.model.Pact currentPact;
    private static Interaction interaction;
    private static Future<Response> actualResponseFuture;

    private PactConsumer() {
    }

    public static void verifyPact() {
        Response actualResponse = null;
        try {
            actualResponse = Await.result(actualResponseFuture, Duration.create(1000, TimeUnit.SECONDS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertEquals(FullResponseMatch$.MODULE$, ResponseMatching$.MODULE$.matchRules(interaction.response(), actualResponse));
    }

    static void setCurrentPact(String pactFile, String interactionDescription, String providerState) {
        currentPact = au.com.dius.pact.model.Pact.from(readFileContents(pactFile));
        try {
            interaction = currentPact.interactionFor(interactionDescription, Option.<String>apply(providerState)).get();
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException("Cannot find interaction with description " + interactionDescription + " and provider state " + providerState);
        }
    }

    public static void sendRequest(String host, int port) {
        testPact(interaction, host, port);
    }

    private static void testPact(Interaction interaction, String host, int port) {
        try {
            final ExecutionContextExecutor executionContextExecutor = ExecutionContext$.MODULE$.fromExecutor(Executors.newCachedThreadPool());
            final Request request = new Request(interaction.request().method(),
                    "http://"+host+":"+port + interaction.request().path(),
                    interaction.request().query(),
                    interaction.request().headers(),
                    interaction.request().body(),
                    interaction.request().matchingRules());
            actualResponseFuture = HttpClient.run(request, executionContextExecutor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String readFileContents(String path) {
        try (final InputStream in = PactRunner.class.getResourceAsStream(path);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read = 0;
            while((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        } catch (IOException ex) {
            throw new RuntimeException("Could not read pact file: " + path);
        }
    }
}
