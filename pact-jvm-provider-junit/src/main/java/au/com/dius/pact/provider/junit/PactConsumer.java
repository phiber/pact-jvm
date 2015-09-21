package au.com.dius.pact.provider.junit;

import au.com.dius.pact.model.*;
import au.com.dius.pact.model.dispatch.HttpClient;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import scala.Option;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext$;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class PactConsumer implements TestRule {


    private au.com.dius.pact.model.Pact currentPact;
    private Interaction interaction;
    private Future<Response> actualResponseFuture;

    public PactConsumer(String pactFile) {
        currentPact = Pact.from(readFileContents(pactFile));
    }

    public void verifyPactInteraction() {
        Response actualResponse = null;
        try {
            actualResponse = Await.result(actualResponseFuture, Duration.create(1000, TimeUnit.SECONDS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertEquals(FullResponseMatch$.MODULE$, ResponseMatching$.MODULE$.matchRules(interaction.response(), actualResponse));
    }

    public void sendRequest(String host, int port) {
        try {
            final ExecutionContextExecutor executionContextExecutor = ExecutionContext$.MODULE$.fromExecutor(Executors.newCachedThreadPool());
            final Request request = new Request(interaction.request().method(),
                    "http://"+ host +":"+ port + interaction.request().path(),
                    interaction.request().query(),
                    interaction.request().headers(),
                    interaction.request().body(),
                    interaction.request().matchingRules());
            actualResponseFuture = HttpClient.run(request, executionContextExecutor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void setCurrentPactInteraction(String interactionDescription, String providerState) {
        try {
            interaction = currentPact.interactionFor(interactionDescription, Option.<String>apply(providerState)).get();
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException("Cannot find interaction with description " + interactionDescription + " and provider state " + providerState);
        }
    }

    private String readFileContents(String path) {
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

    @Override
    public Statement apply(Statement statement, Description description) {
        return new PactWrappedStatement(statement, description);
    }

    private class PactWrappedStatement extends Statement {

        private Statement wrappedStatement;
        private Description description;

        public PactWrappedStatement(Statement wrappedStatement, Description description) {
            this.wrappedStatement = wrappedStatement;
            this.description = description;
        }

        @Override
        public void evaluate() throws Throwable {
            PactFile annotation = description.getTestClass().getAnnotation(PactFile.class);
            if (annotation == null) {
                throw new IllegalArgumentException("Test class must be annotated with @" + PactFile.class.getName());
            }
            String pactFile = annotation.value();

            Method testMethod = description.getTestClass().getMethod(description.getMethodName());
            InteractionDescription providerStateAnnotation = testMethod.getAnnotation(InteractionDescription .class);
            if (providerStateAnnotation == null) {
                throw new IllegalStateException("Test method must be annotated with @" + InteractionDescription .class.getName());
            }
            String interactionDescription = providerStateAnnotation.description();
            String providerState = providerStateAnnotation.providerState();
            if (providerState.isEmpty()) {
                providerState = null;
            }

            setCurrentPactInteraction(interactionDescription, providerState);

            wrappedStatement.evaluate();
        }
    }
}
