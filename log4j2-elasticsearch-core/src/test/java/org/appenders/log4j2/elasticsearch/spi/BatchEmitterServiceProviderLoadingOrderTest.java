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
import org.appenders.log4j2.elasticsearch.TestBatchEmitterFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Spliterator;
import java.util.function.Consumer;

import static org.appenders.log4j2.elasticsearch.BatchDeliveryTest.createTestObjectFactoryBuilder;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class BatchEmitterServiceProviderLoadingOrderTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void firstServiceLoaderWins() {

        // given
        Iterable<BatchEmitterFactory> serviceLoader1 = spy(createTestIterable());

        Iterable<BatchEmitterFactory> serviceLoader2 = ServiceLoader.load(
                BatchEmitterFactory.class,
                TestBatchEmitterFactory.class.getClassLoader());


        BatchEmitter notExpected = mock(BatchEmitter.class);
        TestBatchEmitterFactory factory = new TestBatchEmitterFactory() {
            @Override
            public BatchEmitter createInstance(int batchSize, int deliveryInterval, ClientObjectFactory clientObjectFactory, FailoverPolicy failoverPolicy) {
                return notExpected;
            }
        };
        Iterable<BatchEmitterFactory> serviceLoader3 = spy(createTestIterable(factory));

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
        verify(serviceLoader3, never()).iterator();

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

        verify(serviceLoader1, times(1)).iterator();
        assertSame(expected, instance);

    }

    @Test
    public void checksUntilLastIfPreviousLoaderIsNull() {

        // given
        Iterable<BatchEmitterFactory> serviceLoader2 = createTestIterable(new TestBatchEmitterFactory());

        BatchEmitterServiceProvider serviceProvider = new BatchEmitterServiceProvider(
                Arrays.asList(null, serviceLoader2));

        // when
        serviceProvider.createInstance(
                0,
                0,
                createTestObjectFactoryBuilder().build(),
                new NoopFailoverPolicy());

    }

    private Iterable<BatchEmitterFactory> createTestIterable(TestBatchEmitterFactory testBatchEmitterFactory) {
        return new TestIterable(testBatchEmitterFactory);
    }

    private Iterable<BatchEmitterFactory> createTestIterable() {
        return new TestIterable(null);
    }

    private class TestIterable implements Iterable<BatchEmitterFactory> {

        private final BatchEmitterFactory value;

        public TestIterable(BatchEmitterFactory value) {
            this.value = value;
        }

        @NotNull
        @Override
        public Iterator<BatchEmitterFactory> iterator() {
            return new Iterator<BatchEmitterFactory>() {

                int left = value != null ? 1 : 0;

                @Override
                public boolean hasNext() {
                    return left-- > 0;
                }

                @Override
                public BatchEmitterFactory next() {
                    return value;
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
