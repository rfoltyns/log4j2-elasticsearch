package org.appenders.log4j2.elasticsearch.spi;

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

import org.appenders.log4j2.elasticsearch.BatchEmitter;
import org.appenders.log4j2.elasticsearch.BatchEmitterFactory;
import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;
import org.appenders.log4j2.elasticsearch.NoopFailoverPolicy;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

import static org.appenders.log4j2.elasticsearch.AsyncBatchDeliveryTest.createTestObjectFactoryBuilder;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class BatchEmitterServiceProviderLoadingOrderTest {

    @Test
    public void factoryWithLowestLoadingOrderWins() {

        // given
        BatchEmitter expected = mock(BatchEmitter.class);
        TestBatchEmitterFactory factory1 = new TestBatchEmitterFactory(1) {
            @Override
            public BatchEmitter createInstance(int batchSize, int deliveryInterval, ClientObjectFactory clientObjectFactory, FailoverPolicy failoverPolicy) {
                return expected;
            }
        };
        BatchEmitter notExpected = mock(BatchEmitter.class);
        TestBatchEmitterFactory factory2 = new TestBatchEmitterFactory(2) {
            @Override
            public BatchEmitter createInstance(int batchSize, int deliveryInterval, ClientObjectFactory clientObjectFactory, FailoverPolicy failoverPolicy) {
                return notExpected;
            }
        };

        Iterable<BatchEmitterFactory> serviceLoader1 = spy(createTestIterable(factory1, factory2));

        BatchEmitterServiceProvider serviceProvider = new BatchEmitterServiceProvider(
                Collections.singletonList(serviceLoader1));

        // when
        BatchEmitter instance = serviceProvider.createInstance(
                0,
                0,
                createTestObjectFactoryBuilder().build(),
                new NoopFailoverPolicy());

        assertSame(expected, instance);

    }

    @Test
    public void firstFactoryWithDefaultPriorityWinsIfAllFactoriesHaveDefaultPriority() {

        // given
        BatchEmitter expected = mock(BatchEmitter.class);
        TestBatchEmitterFactory factory1 = new TestBatchEmitterFactory() {
            @Override
            public BatchEmitter createInstance(int batchSize, int deliveryInterval, ClientObjectFactory clientObjectFactory, FailoverPolicy failoverPolicy) {
                return expected;
            }
        };
        BatchEmitter notExpected = mock(BatchEmitter.class);
        TestBatchEmitterFactory factory2 = new TestBatchEmitterFactory() {
            @Override
            public BatchEmitter createInstance(int batchSize, int deliveryInterval, ClientObjectFactory clientObjectFactory, FailoverPolicy failoverPolicy) {
                return notExpected;
            }
        };

        assertSame(new BatchEmitterFactory<BatchEmitter>() {
            @Override
            public boolean accepts(Class<? extends ClientObjectFactory> clientObjectFactoryClass) {
                return false;
            }

            @Override
            public BatchEmitter createInstance(int batchSize, int deliveryInterval, ClientObjectFactory clientObjectFactory, FailoverPolicy failoverPolicy) {
                return null;
            }
        }.loadingOrder(), factory1.loadingOrder());
        assertSame(factory1.loadingOrder(), factory2.loadingOrder());

        Iterable<BatchEmitterFactory> serviceLoader1 = spy(createTestIterable(factory1, factory2));

        BatchEmitterServiceProvider serviceProvider = new BatchEmitterServiceProvider(
                Collections.singletonList(serviceLoader1));

        // when
        BatchEmitter instance = serviceProvider.createInstance(
                0,
                0,
                createTestObjectFactoryBuilder().build(),
                new NoopFailoverPolicy());

        assertSame(expected, instance);

    }

    @Test
    public void firstServiceLoaderWinsIfAllFactoriesHaveTheSamePriority() {

        // given
        Iterable<BatchEmitterFactory> serviceLoader1 = spy(createTestIterable());

        BatchEmitter expected = mock(BatchEmitter.class);
        TestBatchEmitterFactory factory1 = new TestBatchEmitterFactory(1) {
            @Override
            public BatchEmitter createInstance(int batchSize, int deliveryInterval, ClientObjectFactory clientObjectFactory, FailoverPolicy failoverPolicy) {
                return expected;
            }
        };

        BatchEmitter notExpected = mock(BatchEmitter.class);
        TestBatchEmitterFactory factory2 = new TestBatchEmitterFactory(1) {
            @Override
            public BatchEmitter createInstance(int batchSize, int deliveryInterval, ClientObjectFactory clientObjectFactory, FailoverPolicy failoverPolicy) {
                return notExpected;
            }
        };
        Iterable<BatchEmitterFactory> serviceLoader2 = spy(createTestIterable(factory1, factory2));

        TestBatchEmitterFactory factory3 = new TestBatchEmitterFactory(1) {
            @Override
            public BatchEmitter createInstance(int batchSize, int deliveryInterval, ClientObjectFactory clientObjectFactory, FailoverPolicy failoverPolicy) {
                return notExpected;
            }
        };
        Iterable<BatchEmitterFactory> serviceLoader3 = spy(createTestIterable(factory3));

        BatchEmitterServiceProvider serviceProvider = new BatchEmitterServiceProvider(
                Arrays.asList(serviceLoader1, serviceLoader2, serviceLoader3));

        // when
        BatchEmitter instance = serviceProvider.createInstance(
                0,
                0,
                createTestObjectFactoryBuilder().build(),
                new NoopFailoverPolicy());

        // then
        assertNotNull(instance);
        assertNotSame(notExpected, instance);

        verify(serviceLoader1, times(1)).iterator();
        verify(serviceLoader2, times(1)).iterator();
        verify(serviceLoader3, times(1)).iterator();

    }

    @Test
    public void checksUntilLastIfPreviousLoaderIsEmpty() {

        // given
        Iterable<BatchEmitterFactory> serviceLoader1 = spy(createTestIterable());

        BatchEmitter expected = mock(BatchEmitter.class);
        TestBatchEmitterFactory factory = new TestBatchEmitterFactory() {
            @Override
            public BatchEmitter createInstance(int batchSize, int deliveryInterval, ClientObjectFactory clientObjectFactory, FailoverPolicy failoverPolicy) {
                return expected;
            }
        };

        Iterable<BatchEmitterFactory> serviceLoader2 = spy(createTestIterable(factory));

        BatchEmitterServiceProvider serviceProvider = new BatchEmitterServiceProvider(
                Arrays.asList(serviceLoader1, serviceLoader2));

        // when
        BatchEmitter instance = serviceProvider.createInstance(
                0,
                0,
                createTestObjectFactoryBuilder().build(),
                new NoopFailoverPolicy());

        assertSame(expected, instance);

        verify(serviceLoader1, times(1)).iterator();
        verify(serviceLoader2, times(1)).iterator();
    }

    @Test
    public void checksUntilLastIfPreviousFactoryReturnsNull() {

        // given
        TestBatchEmitterFactory factory1 = new TestBatchEmitterFactory(1) {
            @Override
            public BatchEmitter createInstance(int batchSize, int deliveryInterval, ClientObjectFactory clientObjectFactory, FailoverPolicy failoverPolicy) {
                return null;
            }
        };

        BatchEmitter expected = mock(BatchEmitter.class);
        TestBatchEmitterFactory factory2 = new TestBatchEmitterFactory(2) {
            @Override
            public BatchEmitter createInstance(int batchSize, int deliveryInterval, ClientObjectFactory clientObjectFactory, FailoverPolicy failoverPolicy) {
                return expected;
            }
        };
        Iterable<BatchEmitterFactory> serviceLoader1 = spy(createTestIterable(factory1, factory2));

        TestBatchEmitterFactory factory3 = new TestBatchEmitterFactory(3) {
            @Override
            public BatchEmitter createInstance(int batchSize, int deliveryInterval, ClientObjectFactory clientObjectFactory, FailoverPolicy failoverPolicy) {
                return null;
            }
        };

        TestBatchEmitterFactory factory4 = new TestBatchEmitterFactory(4) {
            @Override
            public BatchEmitter createInstance(int batchSize, int deliveryInterval, ClientObjectFactory clientObjectFactory, FailoverPolicy failoverPolicy) {
                return null;
            }
        };
        Iterable<BatchEmitterFactory> serviceLoader2 = spy(createTestIterable(factory4, factory3));

        BatchEmitterServiceProvider serviceProvider = new BatchEmitterServiceProvider(
                Arrays.asList(serviceLoader2, serviceLoader1));

        // when
        BatchEmitter instance = serviceProvider.createInstance(
                0,
                0,
                createTestObjectFactoryBuilder().build(),
                new NoopFailoverPolicy());

        assertSame(expected, instance);

        verify(serviceLoader1, times(1)).iterator();
        verify(serviceLoader2, times(1)).iterator();
    }

    @Test
    public void checksUntilLastIfPreviousLoaderIsNull() {

        // given
        final TestBatchEmitterFactory batchEmitterFactory = new TestBatchEmitterFactory();
        Iterable<BatchEmitterFactory> serviceLoader2 = createTestIterable(batchEmitterFactory);

        BatchEmitterServiceProvider serviceProvider = new BatchEmitterServiceProvider(
                Arrays.asList(null, serviceLoader2));

        // when
        final BatchEmitter instance = serviceProvider.createInstance(
                0,
                0,
                createTestObjectFactoryBuilder().build(),
                new NoopFailoverPolicy());

        // then
        assertSame(batchEmitterFactory.spiedEmitter, instance);

    }

    private Iterable<BatchEmitterFactory> createTestIterable(TestBatchEmitterFactory... testBatchEmitterFactory) {
        return new TestIterable(testBatchEmitterFactory);
    }

    private Iterable<BatchEmitterFactory> createTestIterable() {
        return new TestIterable(null);
    }

    private static class TestIterable implements Iterable<BatchEmitterFactory> {

        private int currentIndex = 0;
        private final BatchEmitterFactory[] values;

        public TestIterable(BatchEmitterFactory... values) {
            this.values = values;
        }

        @NotNull
        @Override
        public Iterator<BatchEmitterFactory> iterator() {
            return new Iterator<BatchEmitterFactory>() {

                @Override
                public boolean hasNext() {
                    if (values == null) {
                        return false;
                    }
                    return currentIndex < values.length;
                }

                @Override
                public BatchEmitterFactory next() {
                    return values[currentIndex++];
                }
            };
        }

        @Override
        public void forEach(Consumer<? super BatchEmitterFactory> action) {
            throw new UnsupportedOperationException("irrelevant here");
        }

        @Override
        public Spliterator<BatchEmitterFactory> spliterator() {
            throw new UnsupportedOperationException("irrelevant here");
        }
    }

}
