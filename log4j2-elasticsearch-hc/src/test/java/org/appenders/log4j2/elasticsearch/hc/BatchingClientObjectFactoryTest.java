package org.appenders.log4j2.elasticsearch.hc;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2021 Rafal Foltynski
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

import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.appenders.log4j2.elasticsearch.ClientFactory;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.appenders.log4j2.elasticsearch.OperationFactory;
import org.appenders.log4j2.elasticsearch.failover.FailedItemOps;
import org.junit.Test;

import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class BatchingClientObjectFactoryTest {

    @Test
    public void lifecycleStartDoesntStartClientProvider() {

        // given
        HttpClientProvider clientProvider = mock(HttpClientProvider.class);
        BatchingClientObjectFactory<BatchRequest, IndexRequest> objectFactory = createDefaultBatchingObjectFactoryBuilder()
                .withClientProvider(clientProvider)
                .build();

        // when
        objectFactory.start();
        objectFactory.start();

        // then
        assertEquals(0, mockingDetails(clientProvider).getInvocations().size());

    }

    @Test
    public void lifecycleStopStopsClientProviderOnlyOnce() {

        // given
        HttpClientProvider clientProvider = mock(HttpClientProvider.class);
        BatchingClientObjectFactory<BatchRequest, IndexRequest> objectFactory = spy(createDefaultBatchingObjectFactoryBuilder()
                .withClientProvider(clientProvider)
                .build());

        objectFactory.start();

        // when
        objectFactory.stop();
        objectFactory.stop();

        // then
        verify(clientProvider).stop();

    }

    @Test
    public void lifecycleStart() {

        // given
        LifeCycle lifeCycle = createLifeCycleTestObject();

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
        LifeCycle lifeCycle = createLifeCycleTestObject();

        assertTrue(lifeCycle.isStopped());

        lifeCycle.start();
        assertTrue(lifeCycle.isStarted());

        ((ClientFactory<HttpClient>)lifeCycle).createClient();

        // when
        lifeCycle.stop();

        // then
        assertFalse(lifeCycle.isStarted());
        assertTrue(lifeCycle.isStopped());

    }

    private LifeCycle createLifeCycleTestObject() {
        return createDefaultBatchingObjectFactoryBuilder().build();
    }

    private BatchingClientObjectFactory.Builder<BatchRequest, IndexRequest> createDefaultBatchingObjectFactoryBuilder() {
        return new BatchingClientObjectFactory.Builder<BatchRequest, IndexRequest>() {
            @Override
            public BatchingClientObjectFactory<BatchRequest, IndexRequest> build() {
                return new BatchingClientObjectFactory<BatchRequest, IndexRequest>(this) {
                    @Override
                    protected ResponseHandler<BatchResult> createResultHandler(BatchRequest request, Function<BatchRequest, Boolean> failureHandler) {
                        return null;
                    }

                    @Override
                    public BatchOperations<BatchRequest> createBatchOperations() {
                        return null;
                    }

                    @Override
                    public void execute(IndexTemplate indexTemplate) {

                    }

                    @Override
                    public OperationFactory setupOperationFactory() {
                        return null;
                    }
                };
            }

            @Override
            protected FailedItemOps<IndexRequest> createFailedItemOps() {
                return null;
            }
        };
    }

}
