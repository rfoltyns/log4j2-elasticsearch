package org.appenders.log4j2.elasticsearch.ahc;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2022 Rafal Foltynski
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.appenders.log4j2.elasticsearch.ByteBufItemSource;
import org.appenders.log4j2.elasticsearch.ByteBufItemSourceTest;
import org.appenders.log4j2.elasticsearch.ComponentTemplate;
import org.appenders.log4j2.elasticsearch.ComponentTemplateTest;
import org.appenders.log4j2.elasticsearch.DataStream;
import org.appenders.log4j2.elasticsearch.DataStreamPlugin;
import org.appenders.log4j2.elasticsearch.EmptyItemSourceFactory;
import org.appenders.log4j2.elasticsearch.ILMPolicy;
import org.appenders.log4j2.elasticsearch.ILMPolicyPluginTest;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.IndexTemplateTest;
import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.appenders.log4j2.elasticsearch.Operation;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.Result;
import org.appenders.log4j2.elasticsearch.SetupStep;
import org.appenders.log4j2.elasticsearch.StepProcessor;
import org.appenders.log4j2.elasticsearch.ValueResolver;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ElasticsearchOperationFactoryTest {

    @Test
    public void supportsComponentTemplate() throws Exception {

        // given
        final String expectedName = UUID.randomUUID().toString();
        final String unresolvedSource = String.format("%s${unresolved}%s", "test", "componentTemplate");
        final ComponentTemplate componentTemplate = ComponentTemplateTest.createTestComponentTemplateBuilder()
                .withName(expectedName)
                .withSource(unresolvedSource)
                .withPath(null)
                .build();

        final CapturingStepProcessor stepProcessor = new CapturingStepProcessor();

        final String expectedResolvedValue = UUID.randomUUID().toString();
        final ValueResolver valueResolver = mock(ValueResolver.class);
        when(valueResolver.resolve(unresolvedSource)).thenReturn(String.format("%s%s%s", "test", expectedResolvedValue, "componentTemplate"));

        final ElasticsearchOperationFactory ops = createDefaultTestOperationsFactoryDispatcher(stepProcessor, valueResolver, ByteBufItemSourceTest::createTestItemSource);

        // when
        final Operation result = ops.create(componentTemplate);
        result.execute();

        // then
        final SetupStep<Request, Response> request = stepProcessor.requests.get(0);
        assertTrue(request instanceof PutComponentTemplate);
        assertEquals(expectedName, ((PutComponentTemplate) request).name);
        final ByteBufItemSource source = (ByteBufItemSource) ((PutComponentTemplate) request).source;
        assertEquals(String.format("%s%s%s", "test", expectedResolvedValue, "componentTemplate"), source.getSource().toString(StandardCharsets.UTF_8));

    }

    private ElasticsearchOperationFactory createDefaultTestOperationsFactoryDispatcher(final CapturingStepProcessor stepProcessor, final ValueResolver valueResolver, final EmptyItemSourceFactory createTestItemSource) {
        return new ElasticsearchOperationFactory(stepProcessor, valueResolver, createTestItemSource);
    }

    @Test
    public void supportsIndexTemplate() throws Exception {

        // given
        final String expectedName = UUID.randomUUID().toString();
        final String unresolvedSource = String.format("%s${unresolved}%s", "test", "indexTemplate");
        final IndexTemplate indexTemplate = IndexTemplateTest.createTestIndexTemplateBuilder()
                .withName(expectedName)
                .withSource(unresolvedSource)
                .withPath(null)
                .build();

        final CapturingStepProcessor stepProcessor = new CapturingStepProcessor();

        final String expectedResolvedValue = UUID.randomUUID().toString();
        final ValueResolver valueResolver = mock(ValueResolver.class);
        when(valueResolver.resolve(unresolvedSource)).thenReturn(String.format("%s%s%s", "test", expectedResolvedValue, "indexTemplate"));

        final ElasticsearchOperationFactory ops = createDefaultTestOperationsFactoryDispatcher(stepProcessor, valueResolver, ByteBufItemSourceTest::createTestItemSource);

        // when
        final Operation result = ops.create(indexTemplate);
        result.execute();

        // then
        final SetupStep<Request, Response> request = stepProcessor.requests.get(0);
        assertTrue(request instanceof PutIndexTemplate);
        assertEquals(expectedName, ((PutIndexTemplate) request).name);
        final ByteBufItemSource source = (ByteBufItemSource) ((PutIndexTemplate) request).source;
        assertEquals(String.format("%s%s%s", "test", expectedResolvedValue, "indexTemplate"), source.getSource().toString(StandardCharsets.UTF_8));

    }

    @Test
    public void supportsILMPolicy() throws Exception {

        // given
        final String expectedName = UUID.randomUUID().toString();
        final String expectedRolloverAlias = UUID.randomUUID().toString();
        final String expectedBootstrapName = expectedRolloverAlias + "-000001";
        final String unresolvedSource = String.format("%s${unresolved}%s", "test", "ilmPolicy");
        final ILMPolicy ilmPolicy = ILMPolicyPluginTest.createTestILMPolicyPluginBuilder()
                .withName(expectedName)
                .withRolloverAlias(expectedRolloverAlias)
                .withSource(unresolvedSource)
                .withPath(null)
                .build();

        final CapturingStepProcessor stepProcessor = new CapturingStepProcessor();

        final String expectedResolvedValue = UUID.randomUUID().toString();
        final ValueResolver valueResolver = mock(ValueResolver.class);
        when(valueResolver.resolve(unresolvedSource)).thenReturn(String.format("%s%s%s", "test", expectedResolvedValue, "ilmPolicy"));

        final ElasticsearchOperationFactory ops = createDefaultTestOperationsFactoryDispatcher(stepProcessor, valueResolver, ByteBufItemSourceTest::createTestItemSource);

        // when
        final Operation result = ops.create(ilmPolicy);
        result.execute();

        // then
        assertEquals(3, stepProcessor.requests.size());

        final SetupStep<Request, Response> checkBootstrapIndex = stepProcessor.requests.get(0);
        assertTrue(checkBootstrapIndex instanceof CheckBootstrapIndex);
        assertEquals(expectedRolloverAlias, checkBootstrapIndex.createRequest().getURI());

        final SetupStep<Request, Response> createBootstrapIndex = stepProcessor.requests.get(1);
        assertTrue(createBootstrapIndex instanceof CreateBootstrapIndex);
        final ByteBufItemSource createBootstrapIndexSource = (ByteBufItemSource) ((CreateBootstrapIndex) createBootstrapIndex).itemSource;
        assertEquals(
                String.format(CreateBootstrapIndex.BOOTSTRAP_TEMPLATE, expectedRolloverAlias),
                createBootstrapIndexSource.getSource().toString(StandardCharsets.UTF_8)
        );
        assertEquals(expectedRolloverAlias, ((CreateBootstrapIndex) createBootstrapIndex).rolloverAlias);
        assertEquals(expectedBootstrapName, ((CreateBootstrapIndex) createBootstrapIndex).bootstrapIndexName);
        assertEquals(expectedBootstrapName, createBootstrapIndex.createRequest().getURI());

        final SetupStep<Request, Response> putIlmPolicy = stepProcessor.requests.get(2);
        assertTrue(putIlmPolicy instanceof PutILMPolicy);

        assertEquals(expectedName, ((PutILMPolicy) putIlmPolicy).name);
        final ByteBufItemSource source = (ByteBufItemSource) ((PutILMPolicy) putIlmPolicy).source;
        assertEquals(String.format("%s%s%s", "test", expectedResolvedValue, "ilmPolicy"), source.getSource().toString(StandardCharsets.UTF_8));
        assertEquals("_ilm/policy/" + expectedName, putIlmPolicy.createRequest().getURI());

    }

    @Test
    public void supportsNonBootstrappingILMPolicy() throws Exception {

        // given
        final String expectedName = UUID.randomUUID().toString();
        final String unresolvedSource = String.format("%s${unresolved}%s", "test", "ilmPolicy");
        final ILMPolicy ilmPolicy = ILMPolicyPluginTest.createTestILMPolicyPluginBuilder()
                .withName(expectedName)
                .withCreateBootstrapIndex(false)
                .withSource(unresolvedSource)
                .withPath(null)
                .build();

        final CapturingStepProcessor stepProcessor = new CapturingStepProcessor();

        final String expectedResolvedValue = UUID.randomUUID().toString();
        final ValueResolver valueResolver = mock(ValueResolver.class);
        when(valueResolver.resolve(unresolvedSource)).thenReturn(String.format("%s%s%s", "test", expectedResolvedValue, "ilmPolicy"));

        final ElasticsearchOperationFactory ops = createDefaultTestOperationsFactoryDispatcher(stepProcessor, valueResolver, ByteBufItemSourceTest::createTestItemSource);

        // when
        final Operation result = ops.create(ilmPolicy);
        result.execute();

        // then
        assertEquals(1, stepProcessor.requests.size());
        final SetupStep<Request, Response> putIlmPolicy = stepProcessor.requests.get(0);
        assertTrue(putIlmPolicy instanceof PutILMPolicy);

        assertEquals(expectedName, ((PutILMPolicy) putIlmPolicy).name);
        final ByteBufItemSource source = (ByteBufItemSource) ((PutILMPolicy) putIlmPolicy).source;
        assertEquals(String.format("%s%s%s", "test", expectedResolvedValue, "ilmPolicy"), source.getSource().toString(StandardCharsets.UTF_8));
        assertEquals("_ilm/policy/" + expectedName, putIlmPolicy.createRequest().getURI());

    }

    @Test
    public void supportsDataStream() throws Exception {

        // given
        final String expectedName = UUID.randomUUID().toString();
        final DataStream dataStream = DataStreamPlugin.newBuilder()
                .withName(expectedName)
                .build();


        final CapturingStepProcessor stepProcessor = new CapturingStepProcessor();

        final ValueResolver valueResolver = mock(ValueResolver.class);

        final EmptyItemSourceFactory testItemSource = ByteBufItemSourceTest::createTestItemSource;
        final ElasticsearchOperationFactory ops = createDefaultTestOperationsFactoryDispatcher(stepProcessor, valueResolver, testItemSource);


        // when
        final Operation result = ops.create(dataStream);
        result.execute();

        // then
        assertEquals(2, stepProcessor.requests.size());
        final SetupStep<Request, Response> checkDataStream = stepProcessor.requests.get(0);
        assertTrue(checkDataStream instanceof CheckDataStream);
        assertNull(checkDataStream.createRequest().serialize());

        assertEquals("_data_stream/" + expectedName, checkDataStream.createRequest().getURI());
        final SetupStep<Request, Response> createDataStream = stepProcessor.requests.get(1);
        assertTrue(createDataStream instanceof CreateDataStream);

        final ByteBufItemSource source = (ByteBufItemSource) ((CreateDataStream) createDataStream).itemSource;
        assertEquals("", source.getSource().toString(StandardCharsets.UTF_8));
        assertEquals(expectedName, ((CreateDataStream) createDataStream).name);
        assertEquals("_data_stream/" + expectedName, createDataStream.createRequest().getURI());

    }

    // =========
    // LIFECYCLE
    // =========

    @Test
    public void lifecycleStartStartItemSourceFactoryOnlyOnce() {

        // given
        final CapturingStepProcessor stepProcessor = new CapturingStepProcessor();
        final ValueResolver valueResolver = mock(ValueResolver.class);

        final PooledItemSourceFactory itemSourceFactory = mock(PooledItemSourceFactory.class);
        final LifeCycle operationFactory = createDefaultTestOperationsFactoryDispatcher(
                stepProcessor, valueResolver, itemSourceFactory);

        // when
        operationFactory.start();
        operationFactory.start();

        // then
        verify(itemSourceFactory).start();

    }

    @Test
    public void lifecycleStopStopsItemSourceFactoryOnlyOnce() {

        // given
        final CapturingStepProcessor stepProcessor = new CapturingStepProcessor();
        final ValueResolver valueResolver = mock(ValueResolver.class);

        final PooledItemSourceFactory itemSourceFactory = mock(PooledItemSourceFactory.class);
        final LifeCycle operationFactory = createDefaultTestOperationsFactoryDispatcher(
                stepProcessor, valueResolver, itemSourceFactory);

        operationFactory.start();

        // when
        operationFactory.stop();
        operationFactory.stop();

        // then
        verify(itemSourceFactory).stop();

    }

    @Test
    public void lifecycleStart() {

        // given
        final LifeCycle lifeCycle = createLifeCycleTestObject();

        assertTrue(lifeCycle.isStopped());

        // when
        lifeCycle.start();

        // then
        assertFalse(lifeCycle.isStopped());
        assertTrue(lifeCycle.isStarted());

    }

    @Test
    public void lifecycleStop() {

        // given
        final LifeCycle lifeCycle = createLifeCycleTestObject();

        assertTrue(lifeCycle.isStopped());

        lifeCycle.start();
        assertTrue(lifeCycle.isStarted());

        // when
        lifeCycle.stop();

        // then
        assertFalse(lifeCycle.isStarted());
        assertTrue(lifeCycle.isStopped());

    }

    private LifeCycle createLifeCycleTestObject() {
        return createDefaultTestOperationsFactoryDispatcher(
                new CapturingStepProcessor(),
                mock(ValueResolver.class),
                mock(PooledItemSourceFactory.class));
    }

    private static class CapturingStepProcessor implements StepProcessor<SetupStep<Request, Response>> {

        private final List<SetupStep<Request, Response>> requests = new ArrayList<>();

        @Override
        public Result process(final SetupStep<Request, Response> request) {
            requests.add(request);
            return Result.SUCCESS;
        }

    }

}
