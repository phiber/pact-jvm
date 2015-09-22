package au.com.dius.pact.provider.junit;


import au.com.dius.pact.model.Interaction;
import au.com.dius.pact.model.Pact;
import org.json4s.StreamInput;
import scala.collection.JavaConverters;

import java.io.InputStream;

public class PactConsumerBuilder {

    private Pact pact;
    private Interaction interaction;

    private PactConsumerBuilder(Pact pact) {
        this.pact = pact;
    }

    public static PactConsumerBuilder fromPactFile(String resource) {
        return fromPactFile(PactConsumerBuilder.class.getResourceAsStream(resource));
    }

    public static PactConsumerBuilder fromPactFile(InputStream resourceAsStream) {
        Pact pact = Pact.from(new StreamInput(resourceAsStream));
        return new PactConsumerBuilder(pact);
    }

    public PactConsumerBuilder withInteraction(String description) {
        this.interaction = findInteractionByDescription(description);
        return this;
    }

    private Interaction findInteractionByDescription(String description) {
        // TODO: Scala and Java 8 does not work together?
        // return JavaConverters.seqAsJavaListConverter(pact.interactions()).asJava().stream().findFirst(it -> {
        //      it.description().equals(description);
        // });
        for (Interaction interaction : JavaConverters.seqAsJavaListConverter(pact.interactions()).asJava()) {
            if (interaction.description().equals(description)) {
                return interaction;
            }
        }
        throw new IllegalArgumentException("No interaction with description '" + description + "' found.");
    }

    public PactConsumer build() {
        return new PactConsumer(interaction);
    }
}