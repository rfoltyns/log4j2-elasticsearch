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

import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.IndexTemplateTest;
import org.appenders.log4j2.elasticsearch.OpSource;
import org.appenders.log4j2.elasticsearch.Operation;
import org.appenders.log4j2.elasticsearch.StepProcessor;
import org.appenders.log4j2.elasticsearch.ValueResolver;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class BulkProcessorOperationFactoryDispatcherTest {

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

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> factory.handleMissing(opSource));

        // then
        assertThat(exception.getMessage(), equalTo(TestOpSource.class.getSimpleName() + " is not supported"));

    }

    private static class TestOpSource implements OpSource {

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
