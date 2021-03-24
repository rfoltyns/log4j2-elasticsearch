package org.appenders.log4j2.elasticsearch;

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

import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;
import org.jctools.queues.MpmcUnboundedXaddArrayQueue;
import org.jctools.queues.MpscUnboundedArrayQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.appenders.core.logging.InternalLoggingTest.mockTestLogger;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class QueueFactoryTest {

    private static final int DEFAULT_TEST_INITIAL_SIZE = 10;

    @SuppressWarnings("rawtypes")
    private final Class mpscQueueClass = MpscUnboundedArrayQueue.class;

    @SuppressWarnings("rawtypes")
    private final Class mpmcQueueClass = MpmcUnboundedXaddArrayQueue.class;

    @SuppressWarnings("rawtypes")
    private final Class fallbackQueueClass = ConcurrentLinkedQueue.class;

    @AfterEach
    public void tearDown() {
        InternalLogging.setLogger(null);
    }

    @Test
    public void shouldCreateMpmcQueueIfEnabled() {

        // given
        String name = UUID.randomUUID().toString();
        System.setProperty(String.format("appenders.%s.jctools.enabled", name), "true");

        // when
        Queue<Object> queue = createDefaultTestFactory().tryCreateMpmcQueue(name, DEFAULT_TEST_INITIAL_SIZE);

        // then
        assertSame(mpmcQueueClass, queue.getClass());

    }

    @Test
    public void shouldCreateMpscQueueIfEnabled() {

        // given
        String name = UUID.randomUUID().toString();
        System.setProperty(String.format("appenders.%s.jctools.enabled", name), "true");

        // when
        Queue<Object> queue = createDefaultTestFactory().tryCreateMpscQueue(name, DEFAULT_TEST_INITIAL_SIZE);

        // then
        assertSame(mpscQueueClass, queue.getClass());

    }

    @Test
    public void shouldNotCreateMpmcQueueIfDisabled() {

        // given
        String name = UUID.randomUUID().toString();
        System.setProperty(String.format("appenders.%s.jctools.enabled", name), "false");

        // when
        Queue<Object> queue = createDefaultTestFactory().tryCreateMpmcQueue(name, DEFAULT_TEST_INITIAL_SIZE);

        // then
        assertNotSame(mpmcQueueClass, queue.getClass());

    }

    @Test
    public void shouldNotCreateMpscQueueIfDisabled() {

        // given
        String name = UUID.randomUUID().toString();
        System.setProperty(String.format("appenders.%s.jctools.enabled", name), "false");

        // when
        Queue<Object> queue = createDefaultTestFactory().tryCreateMpscQueue(name, DEFAULT_TEST_INITIAL_SIZE);

        // then
        assertNotSame(mpscQueueClass, queue.getClass());

    }

    @Test
    public void shouldNotCreateMpmcQueueIfClassNotFound() {

        // given
        String name = UUID.randomUUID().toString();
        System.setProperty(String.format("appenders.%s.jctools.enabled", name), "true");

        QueueFactory queueFactory = spy(createDefaultTestFactory());
        when(queueFactory.hasClass(name, mpmcQueueClass.getName())).thenReturn(false);

        Logger logger = mockTestLogger();

        // when
        Queue<Object> queue = queueFactory.tryCreateMpmcQueue(name, DEFAULT_TEST_INITIAL_SIZE);

        // then
        assertNotSame(mpmcQueueClass, queue.getClass());
        verify(logger).debug("{}: Falling back to {}", name, fallbackQueueClass.getName());

    }

    @Test
    public void shouldNotCreateMpscQueueIfClassNotFound() {

        // given
        String name = UUID.randomUUID().toString();
        System.setProperty(String.format("appenders.%s.jctools.enabled", name), "true");

        QueueFactory queueFactory = spy(createDefaultTestFactory());
        when(queueFactory.hasClass(name, mpscQueueClass.getName())).thenReturn(false);

        Logger logger = mockTestLogger();

        // when
        Queue<Object> queue = queueFactory.tryCreateMpscQueue(name, DEFAULT_TEST_INITIAL_SIZE);

        // then
        assertNotSame(mpscQueueClass, queue.getClass());
        verify(logger).debug("{}: Falling back to {}", name, fallbackQueueClass.getName());

    }

    @Test
    public void mpmcQueueShouldResize() {

        // given
        String name = UUID.randomUUID().toString();
        System.setProperty(String.format("appenders.%s.jctools.enabled", name), "true");

        QueueFactory queueFactory = spy(createDefaultTestFactory());

        Queue<Object> queue = queueFactory.tryCreateMpmcQueue(name, DEFAULT_TEST_INITIAL_SIZE);

        // when
        for (int i = 0; i < 10000; i++) {
            queue.offer(new Object());
        }

        // then
        assertSame(mpmcQueueClass, queue.getClass());
        assertEquals(10000, queue.size());

    }

    @Test
    public void mpscQueueShouldResize() {

        // given
        String name = UUID.randomUUID().toString();
        System.setProperty(String.format("appenders.%s.jctools.enabled", name), "true");

        QueueFactory queueFactory = createDefaultTestFactory();
        Queue<Object> queue = queueFactory.tryCreateMpscQueue(name, DEFAULT_TEST_INITIAL_SIZE);

        // when
        for (int i = 0; i < 10000; i++) {
            queue.offer(new Object());
        }

        // then
        assertSame(mpscQueueClass, queue.getClass());
        assertEquals(10000, queue.size());

    }

    @Test
    public void throwsOnNonSupportedClass() {

        // given
        String caller = UUID.randomUUID().toString();
        String className = UUID.randomUUID().toString();

        QueueFactory queueFactory = spy(createDefaultTestFactory());
        when(queueFactory.hasClass(caller, className)).thenReturn(true);

        // when
        final UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> queueFactory.tryCreate(caller, className, DEFAULT_TEST_INITIAL_SIZE));

        // then
        assertThat(exception.getMessage(), containsString(className + " is not supported"));

    }

    @Test
    public void classCheckLogs() {

        // given
        QueueFactory queueFactory = createDefaultTestFactory();
        Logger logger = mockTestLogger();

        String caller = UUID.randomUUID().toString();
        String className = UUID.randomUUID().toString();

        // when
        queueFactory.hasClass(caller, className);

        // then
        verify(logger).debug("{}: {} not available", caller, className);

    }

    @Test
    public void canConvertNonIterable() {

        // given
        QueueFactory queueFactory = createDefaultTestFactory();

        Queue<Object> mpmcQueue = queueFactory.tryCreateMpmcQueue(UUID.randomUUID().toString(), DEFAULT_TEST_INITIAL_SIZE);

        assertSame(mpmcQueueClass, mpmcQueue.getClass());

        // when
        Collection<Object> iterable = queueFactory.toIterable(mpmcQueue);

        // then
        assertSame(ArrayList.class, iterable.getClass());

    }

    @Test
    public void convertNonIterableCopiesAllItems() {

        // given
        QueueFactory queueFactory = createDefaultTestFactory();

        Queue<Object> mpmcQueue = queueFactory.tryCreateMpmcQueue(UUID.randomUUID().toString(), DEFAULT_TEST_INITIAL_SIZE);

        assertSame(mpmcQueueClass, mpmcQueue.getClass());

        mpmcQueue.add(new Object());
        mpmcQueue.add(new Object());

        // when
        Collection<Object> iterable = queueFactory.toIterable(mpmcQueue);

        // then
        assertEquals(2, iterable.size());

    }

    @Test
    public void doesNotConvertFallbackQueue() {

        // given
        String name = UUID.randomUUID().toString();
        System.setProperty(String.format("appenders.%s.jctools.enabled", name), "false");
        QueueFactory queueFactory = createDefaultTestFactory();

        Queue<Object> mpmcQueue = queueFactory.tryCreateMpmcQueue(name, DEFAULT_TEST_INITIAL_SIZE);

        assertSame(fallbackQueueClass, mpmcQueue.getClass());

        // when
        Collection<Object> iterable = queueFactory.toIterable(mpmcQueue);

        // then
        assertSame(mpmcQueue, iterable);

    }

    @Test
    public void doesNotConvertNonQueues() {

        // given
        String name = UUID.randomUUID().toString();
        System.setProperty(String.format("appenders.%s.jctools.enabled", name), "false");
        QueueFactory queueFactory = createDefaultTestFactory();

        Collection<Object> collection = new ArrayList<>();

        // when
        Collection<Object> iterable = queueFactory.toIterable(collection);

        // then
        assertSame(collection, iterable);

    }

    private QueueFactory createDefaultTestFactory() {
        return new QueueFactory();
    }

}
