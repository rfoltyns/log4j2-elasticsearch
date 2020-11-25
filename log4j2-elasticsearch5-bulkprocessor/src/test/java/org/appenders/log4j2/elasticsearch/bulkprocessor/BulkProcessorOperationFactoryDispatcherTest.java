package org.appenders.log4j2.elasticsearch.bulkprocessor;

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

import org.appenders.log4j2.elasticsearch.ILMPolicy;
import org.appenders.log4j2.elasticsearch.ILMPolicyPluginTest;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.IndexTemplateTest;
import org.appenders.log4j2.elasticsearch.OpSource;
import org.appenders.log4j2.elasticsearch.Operation;
import org.appenders.log4j2.elasticsearch.StepProcessor;
import org.appenders.log4j2.elasticsearch.ValueResolver;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class BulkProcessorOperationFactoryDispatcherTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void createsIndexTemplate() {

        // given
        BulkProcessorOperationFactoryDispatcher factory = spy(new BulkProcessorOperationFactoryDispatcher(
                mock(StepProcessor.class),
                ValueResolver.NO_OP
        ));

        IndexTemplate opSource = spy(IndexTemplateTest.createTestIndexTemplateBuilder().build());

        // when
        Operation operation = factory.create(opSource);

        // then
        assertNotNull(operation);

    }

    @Test
    public void handleUnsupportedThrowsByDefault() {

        // given
        BulkProcessorOperationFactoryDispatcher factory = new BulkProcessorOperationFactoryDispatcher(
                mock(StepProcessor.class),
                ValueResolver.NO_OP
        );

        OpSource opSource = new TestOpSource();

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(TestOpSource.class.getSimpleName() + " is not supported");

        // when
        factory.handleMissing(opSource);

    }

    private class TestOpSource implements OpSource {

        @Override
        public String getType() {
            return "missing";
        }

        @Override
        public String getSource() {
            return UUID.randomUUID().toString();
        }

    }

}
