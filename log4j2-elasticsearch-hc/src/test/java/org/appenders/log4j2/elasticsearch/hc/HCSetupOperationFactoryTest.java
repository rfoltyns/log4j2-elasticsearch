package org.appenders.log4j2.elasticsearch.hc;

import org.appenders.log4j2.elasticsearch.ByteBufItemSource;
import org.appenders.log4j2.elasticsearch.ByteBufItemSourceTest;
import org.appenders.log4j2.elasticsearch.ILMPolicy;
import org.appenders.log4j2.elasticsearch.ILMPolicyPluginTest;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.IndexTemplateTest;
import org.appenders.log4j2.elasticsearch.Operation;
import org.appenders.log4j2.elasticsearch.Result;
import org.appenders.log4j2.elasticsearch.SetupStep;
import org.appenders.log4j2.elasticsearch.StepProcessor;
import org.appenders.log4j2.elasticsearch.ValueResolver;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HCSetupOperationFactoryTest {

    @Test
    public void supportsIndexTemplate() throws Exception {

        // given
        String expectedName = UUID.randomUUID().toString();
        String unresolvedSource = String.format("%s${unresolved}%s", "test", "indexTemplate");
        IndexTemplate indexTemplate = IndexTemplateTest.createTestIndexTemplateBuilder()
                .withName(expectedName)
                .withSource(unresolvedSource)
                .withPath(null)
                .build();

        CapturingStepProcessor stepProcessor = new CapturingStepProcessor();

        String expectedResolvedValue = UUID.randomUUID().toString();
        ValueResolver valueResolver = mock(ValueResolver.class);
        when(valueResolver.resolve(unresolvedSource)).thenReturn(String.format("%s%s%s", "test", expectedResolvedValue, "indexTemplate"));

        HCSetupOperationFactory ops = new HCSetupOperationFactory(stepProcessor, valueResolver, ByteBufItemSourceTest::createTestItemSource);

        // when
        Operation result = ops.indexTemplate(indexTemplate);
        result.execute();

        // then
        SetupStep<Request, Response> request = stepProcessor.requests.get(0);
        assertTrue(request instanceof PutIndexTemplate);
        assertEquals(expectedName, ((PutIndexTemplate) request).name);
        ByteBufItemSource source = (ByteBufItemSource) ((PutIndexTemplate) request).source;
        assertEquals(String.format("%s%s%s", "test", expectedResolvedValue, "indexTemplate"), source.getSource().toString(StandardCharsets.UTF_8));

    }

    @Test
    public void supportsILMPolicy() throws Exception {

        // given
        String expectedName = UUID.randomUUID().toString();
        String expectedRolloverAlias = UUID.randomUUID().toString();
        String expectedBootstrapName = expectedRolloverAlias + "-000001";
        String unresolvedSource = String.format("%s${unresolved}%s", "test", "ilmPolicy");
        ILMPolicy ilmPolicy = ILMPolicyPluginTest.createTestILMPolicyPluginBuilder()
                .withName(expectedName)
                .withRolloverAlias(expectedRolloverAlias)
                .withSource(unresolvedSource)
                .withPath(null)
                .build();

        CapturingStepProcessor stepProcessor = new CapturingStepProcessor();

        String expectedResolvedValue = UUID.randomUUID().toString();
        ValueResolver valueResolver = mock(ValueResolver.class);
        when(valueResolver.resolve(unresolvedSource)).thenReturn(String.format("%s%s%s", "test", expectedResolvedValue, "ilmPolicy"));

        HCSetupOperationFactory ops = new HCSetupOperationFactory(stepProcessor, valueResolver, ByteBufItemSourceTest::createTestItemSource);

        // when
        Operation result = ops.ilmPolicy(ilmPolicy);
        result.execute();

        // then
        SetupStep<Request, Response> checkBootstrapIndex = stepProcessor.requests.get(0);
        assertTrue(checkBootstrapIndex instanceof CheckBootstrapIndex);
        assertEquals(expectedRolloverAlias, checkBootstrapIndex.createRequest().getURI());

        SetupStep<Request, Response> createBootstrapIndex = stepProcessor.requests.get(1);
        assertTrue(createBootstrapIndex instanceof CreateBootstrapIndex);
        ByteBufItemSource createBootstrapIndexSource = (ByteBufItemSource) ((CreateBootstrapIndex) createBootstrapIndex).itemSource;
        assertEquals(
                String.format(CreateBootstrapIndex.BOOTSTRAP_TEMPLATE, expectedRolloverAlias),
                createBootstrapIndexSource.getSource().toString(StandardCharsets.UTF_8)
        );
        assertEquals(expectedRolloverAlias, ((CreateBootstrapIndex) createBootstrapIndex).rolloverAlias);
        assertEquals(expectedBootstrapName, ((CreateBootstrapIndex) createBootstrapIndex).bootstrapIndexName);
        assertEquals(expectedBootstrapName, createBootstrapIndex.createRequest().getURI());

        SetupStep<Request, Response> putIlmPolicy = stepProcessor.requests.get(2);
        assertTrue(putIlmPolicy instanceof PutILMPolicy);

        assertEquals(expectedName, ((PutILMPolicy) putIlmPolicy).name);
        ByteBufItemSource source = (ByteBufItemSource) ((PutILMPolicy) putIlmPolicy).source;
        assertEquals(String.format("%s%s%s", "test", expectedResolvedValue, "ilmPolicy"), source.getSource().toString(StandardCharsets.UTF_8));
        assertEquals("_ilm/policy/" + expectedName, putIlmPolicy.createRequest().getURI());

    }

    private static class CapturingStepProcessor implements StepProcessor<SetupStep<Request, Response>> {

        private final List<SetupStep<Request, Response>> requests = new ArrayList<>();

        @Override
        public Result process(SetupStep<Request, Response> request) {
            requests.add(request);
            return Result.SUCCESS;
        }

    }

}