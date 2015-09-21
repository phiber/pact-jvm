package au.com.dius.pact.provider.junit;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.reflect.Method;

public class PactRule implements TestRule {


    @Override
    public Statement apply(Statement statement, Description description) {
        return new PactWrappedStatement(statement, description);
    }

    private static class PactWrappedStatement extends Statement {

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

            PactConsumer.setCurrentPact(pactFile, interactionDescription, providerState);

            wrappedStatement.evaluate();
        }
    }
}
