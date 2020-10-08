package org.appenders.log4j2.elasticsearch.bulkprocessor;

import org.appenders.log4j2.elasticsearch.ILMPolicy;
import org.appenders.log4j2.elasticsearch.ILMPolicyPluginTest;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.IndexTemplateTest;
import org.appenders.log4j2.elasticsearch.OpSource;
import org.appenders.log4j2.elasticsearch.StepProcessor;
import org.appenders.log4j2.elasticsearch.ValueResolver;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class BulkProcessorSetupOperationFactoryTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void createsIndexTemplate() {

        // given
        BulkProcessorSetupOperationFactory factory = spy(new BulkProcessorSetupOperationFactory(
                mock(StepProcessor.class),
                ValueResolver.NO_OP
        ));

        IndexTemplate opSource = spy(IndexTemplateTest.createTestIndexTemplateBuilder().build());

        // when
        factory.create(opSource);

        // then
        verify(factory).indexTemplate(eq(opSource));

    }

    @Test
    public void ilmPolicyIsNotSupported() {

        // given
        BulkProcessorSetupOperationFactory factory = spy(new BulkProcessorSetupOperationFactory(
                mock(StepProcessor.class),
                ValueResolver.NO_OP
        ));

        ILMPolicy opSource = spy(ILMPolicyPluginTest.createTestILMPolicyPluginBuilder().build());

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(opSource.getClass().getSimpleName() + " is not supported");

        // when
        factory.create(opSource);

        // then
        verify(factory).ilmPolicy(eq(opSource));

    }

    @Test
    public void handleUnsupportedThrowsByDefault() {

        // given
        BulkProcessorSetupOperationFactory factory = new BulkProcessorSetupOperationFactory(
                mock(StepProcessor.class),
                ValueResolver.NO_OP
        );

        OpSource opSource = new TestOpSource();

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(TestOpSource.class.getSimpleName() + " is not supported");

        // when
        factory.handleUnsupported(opSource);

    }

    private class TestOpSource implements OpSource {

        @Override
        public String getType() {
            return UUID.randomUUID().toString();
        }

        @Override
        public String getSource() {
            return UUID.randomUUID().toString();
        }

    }

}