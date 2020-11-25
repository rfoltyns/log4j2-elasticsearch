package org.appenders.log4j2.elasticsearch.jest;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2020 Rafal Foltynski
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

import io.searchbox.action.GenericJestRequestIntrospector;
import io.searchbox.client.JestResult;
import org.appenders.log4j2.elasticsearch.ILMPolicy;
import org.appenders.log4j2.elasticsearch.ILMPolicyPluginTest;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.IndexTemplateTest;
import org.appenders.log4j2.elasticsearch.Operation;
import org.appenders.log4j2.elasticsearch.Result;
import org.appenders.log4j2.elasticsearch.SetupStep;
import org.appenders.log4j2.elasticsearch.StepProcessor;
import org.appenders.log4j2.elasticsearch.ValueResolver;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JestOperationFactoryDispatcherTest {

    @Test
    public void canProduceIndexTemplateSetupOp() throws Exception {

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

        JestOperationFactoryDispatcher ops = new JestOperationFactoryDispatcher(stepProcessor, valueResolver);

        // when
        Operation result = ops.create(indexTemplate);
        result.execute();

        // then
        SetupStep<GenericJestRequest, JestResult> request = assertSetupStep(expectedName, stepProcessor);

        String source = GenericJestRequestIntrospector.getPayload(request.createRequest());
        assertEquals(String.format("%s%s%s", "test", expectedResolvedValue, "indexTemplate"), source);

    }

    @NotNull
    private SetupStep<GenericJestRequest, JestResult> assertSetupStep(String expectedName, CapturingStepProcessor stepProcessor) {
        SetupStep<GenericJestRequest, JestResult> request = stepProcessor.requests.get(0);
        assertTrue(request instanceof PutIndexTemplate);
        assertEquals(expectedName, ((PutIndexTemplate) request).templateName);
        return request;
    }

    @Test
    public void canProduceILMPolicySetupOp() throws Exception {

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

        JestOperationFactoryDispatcher ops = new JestOperationFactoryDispatcher(stepProcessor, valueResolver);

        // when
        Operation result = ops.create(ilmPolicy);
        result.execute();

        // then
        SetupStep<GenericJestRequest, JestResult> checkBootstrapIndex = stepProcessor.requests.get(0);
        assertTrue(checkBootstrapIndex instanceof CheckBootstrapIndex);
        assertEquals(expectedRolloverAlias, checkBootstrapIndex.createRequest().buildURI());

        SetupStep<GenericJestRequest, JestResult> createBootstrapIndex = stepProcessor.requests.get(1);
        assertTrue(createBootstrapIndex instanceof CreateBootstrapIndex);

        String createBootstrapIndexSource = GenericJestRequestIntrospector.getPayload(createBootstrapIndex.createRequest());

        assertEquals(String.format(CreateBootstrapIndex.BOOTSTRAP_TEMPLATE, expectedRolloverAlias), createBootstrapIndexSource);
        assertEquals(expectedRolloverAlias, ((CreateBootstrapIndex) createBootstrapIndex).rolloverAlias);
        assertEquals(expectedBootstrapName, ((CreateBootstrapIndex) createBootstrapIndex).bootstrapIndexName);
        assertEquals(expectedBootstrapName, createBootstrapIndex.createRequest().buildURI());

        SetupStep<GenericJestRequest, JestResult> putIlmPolicy = stepProcessor.requests.get(2);
        assertTrue(putIlmPolicy instanceof PutILMPolicy);

        assertEquals(expectedName, ((PutILMPolicy) putIlmPolicy).name);
        String source = GenericJestRequestIntrospector.getPayload(putIlmPolicy.createRequest());
        assertEquals(String.format("%s%s%s", "test", expectedResolvedValue, "ilmPolicy"), source);
        assertEquals("_ilm/policy/" + expectedName, putIlmPolicy.createRequest().buildURI());

    }

    private static class CapturingStepProcessor implements StepProcessor<SetupStep<GenericJestRequest, JestResult>> {

        private final List<SetupStep<GenericJestRequest, JestResult>> requests = new ArrayList<>();

        @Override
        public Result process(SetupStep<GenericJestRequest, JestResult> request) {
            requests.add(request);
            return Result.SUCCESS;
        }

    }

}
