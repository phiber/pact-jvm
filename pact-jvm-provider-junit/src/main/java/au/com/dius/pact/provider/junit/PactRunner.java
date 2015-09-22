package au.com.dius.pact.provider.junit;

import au.com.dius.pact.model.*;
import au.com.dius.pact.model.dispatch.HttpClient;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;
import scala.collection.JavaConverters;
import scala.collection.Seq;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext$;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class PactRunner extends ParentRunner<Interaction> {

    private final Pact pactToVerify;
    private final Object testInstance;

    public PactRunner(Class<?> klass) throws InitializationError {
        super(klass);
        testInstance = createTestObject(getTestClass());
        pactToVerify = readPactDefinitionFor(klass);
    }

    @Override
    protected List<Interaction> getChildren() {
        final Seq<Interaction> interactions = pactToVerify.interactions();
        return JavaConverters.seqAsJavaListConverter(interactions).asJava();
    }

    @Override
    protected Description describeChild(Interaction interaction) {
        return Description.createTestDescription(pactToVerify.consumer().name(), interaction.description());
    }

    @Override
    protected void runChild(Interaction interaction, RunNotifier notifier) {
        final Description description = describeChild(interaction);
        notifier.fireTestStarted(description);

        if (interaction.providerState().isDefined()) {
            final Optional<FrameworkMethod> initializationMethod = findProvderStateInitializationMethod(interaction.providerState().get());
            if (initializationMethod.isPresent()) {
                try {
                    invokeMethod(initializationMethod.get());
                    testPact(interaction);
                    notifier.fireTestFinished(description);
                    return;
                } catch(Exception ex) {
                    notifier.fireTestFailure(new Failure(description, ex));
                    return;
                }
            } else {
                notifier.fireTestIgnored(description);
                return;
            }
        }
        notifier.fireTestIgnored(description);
    }

    private static void testPact(Interaction interaction) {
        try {
            final ExecutionContextExecutor executionContextExecutor = ExecutionContext$.MODULE$.fromExecutor(Executors.newCachedThreadPool());
            final Request request = new Request(interaction.request().method(),
                    "http://localhost:8080" + interaction.request().path(),
                    interaction.request().query(),
                    interaction.request().headers(),
                    interaction.request().body(),
                    interaction.request().matchingRules());
            Future<Response> actualResponseFuture = HttpClient.run(request, executionContextExecutor);
            Response actualResponse = Await.result(actualResponseFuture, Duration.create(1000, TimeUnit.SECONDS));
            assertEquals(FullResponseMatch$.MODULE$, ResponseMatching$.MODULE$.matchRules(interaction.response(), actualResponse));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Pact readPactDefinitionFor(Class<?> klass) {
        final String pactFile = getPactFileLocation(klass);
        final String pactFileContent = readFileContents(pactFile);
        return Pact.from(pactFileContent);
    }

    private static Object createTestObject(TestClass testClass) throws InitializationError {
        try {
            return testClass.getOnlyConstructor().newInstance();
        } catch (Exception ex) {
            throw new InitializationError(ex);
        }
    }

    private void invokeMethod(FrameworkMethod method) {
        try {
            method.getMethod().invoke(testInstance);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Could not create test class");
        }
    }

    private Optional<FrameworkMethod> findProvderStateInitializationMethod(String providerState) {
        return getTestClass().getAnnotatedMethods(ProviderState.class).stream()
                .filter(method -> method.getAnnotation(ProviderState.class).value().equals(providerState))
                .findFirst();
    }

    private static String getPactFileLocation(Class<?> klass) {
        final PactFile pactFile = klass.getAnnotation(PactFile.class);
        if (pactFile == null)
            throw new RuntimeException("PactRunner requires the method to be annotated with PactFile");

        return pactFile.value();
    }

    private static String readFileContents(String path) {
        try (final InputStream in = PactRunner.class.getResourceAsStream(path)) {
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String str = null;
            StringBuilder sb = new StringBuilder(8192);
            while ((str = r.readLine()) != null) {
                sb.append(str);
            }
            return sb.toString();
        } catch (IOException ex) {
            throw new RuntimeException("Could not read pact file: " + path);
        }
    }

}
