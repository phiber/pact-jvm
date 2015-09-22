package au.com.dius.pact.provider.junit;

import au.com.dius.pact.model.*;
import au.com.dius.pact.model.dispatch.HttpClient;
import scala.collection.Seq;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext$;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public class PactConsumer {

    private Interaction interaction;
    private Future<Response> actualResponseFuture;

    public PactConsumer(Interaction interaction) {
        this.interaction = interaction;
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

    public void verifyPactInteraction() {
        Response actualResponse = null;
        try {
            actualResponse = Await.result(actualResponseFuture, Duration.create(1000, TimeUnit.SECONDS));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Seq<ResponsePartMismatch> responsePartMismatchSeq = new ResponseMatching(new DiffConfig(false, true)).responseMismatches(interaction.response(), actualResponse);

        assertTrue("There where missmatches:\n" + responsePartMismatchSeq , responsePartMismatchSeq .isEmpty());
    }
}
