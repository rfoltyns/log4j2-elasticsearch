package org.appenders.log4j2.elasticsearch.spi;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2018 Rafal Foltynski
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


import org.apache.logging.log4j.core.config.Configuration;
import org.appenders.log4j2.elasticsearch.BatchEmitter;
import org.appenders.log4j2.elasticsearch.BatchEmitterFactory;
import org.appenders.log4j2.elasticsearch.TestHttpObjectFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.ArrayList;
import java.util.Collections;

import static org.appenders.log4j2.elasticsearch.AppenderRefFailoverPolicyTest.createTestFailoverPolicy;
import static org.appenders.log4j2.elasticsearch.AsyncBatchDeliveryTest.createTestObjectFactoryBuilder;
import static org.appenders.log4j2.elasticsearch.BulkEmitterTest.LARGE_TEST_INTERVAL;
import static org.appenders.log4j2.elasticsearch.BulkEmitterTest.TEST_BATCH_SIZE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BatchEmitterServiceProviderTest {

    @Test
    public void throwsWhenNoServiceWasFound() {

        // given
        BatchEmitterServiceProvider serviceProvider = new BatchEmitterServiceProvider(Collections.emptyList());

        // when
        final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> serviceProvider.createInstance(TEST_BATCH_SIZE,
                LARGE_TEST_INTERVAL,
                createTestObjectFactoryBuilder().build(),
                createTestFailoverPolicy("testRefAppender", mock(Configuration.class))
        ));

        // then
        assertThat(exception.getMessage(), containsString("No compatible BatchEmitter implementations"));

    }

    @Test
    public void succeedsWhenCompatibleFactoryWasFound() {

        // given
        BatchEmitterServiceProvider serviceProvider = new BatchEmitterServiceProvider();

        // when
        BatchEmitter emitter = createWithTestValues(serviceProvider);

        // then
        assertNotNull(emitter);

    }

    @Test
    public void throwsWhenFoundFactoryWasIncompatible() {

        // given
        TestBatchEmitterFactory emitterFactory = mock(TestBatchEmitterFactory.class);
        Iterable<BatchEmitterFactory> serviceLoader = new ArrayList<BatchEmitterFactory>() {{
            add(emitterFactory);
        }};
        BatchEmitterServiceProvider serviceProvider = new BatchEmitterServiceProvider(Collections.singletonList(serviceLoader));

        when(emitterFactory.accepts(ArgumentMatchers.<Class<TestHttpObjectFactory>>any())).thenReturn(false);

        // when
        final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> createWithTestValues(serviceProvider));

        // then
        assertThat(exception.getMessage(), containsString("No compatible BatchEmitter implementations"));

    }

    private BatchEmitter createWithTestValues(BatchEmitterServiceProvider serviceProvider) {
        return serviceProvider.createInstance(TEST_BATCH_SIZE,
                LARGE_TEST_INTERVAL,
                createTestObjectFactoryBuilder().build(),
                createTestFailoverPolicy("testRefAppender", mock(Configuration.class))
        );
    }

}


