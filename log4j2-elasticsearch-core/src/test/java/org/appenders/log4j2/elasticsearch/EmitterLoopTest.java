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

import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.AsyncBatchEmitter.EmitterLoop;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static org.appenders.core.logging.InternalLogging.setLogger;
import static org.appenders.core.logging.InternalLoggingTest.mockTestLogger;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class EmitterLoopTest {

    @AfterEach
    public void tearDown() {
        setLogger(null);
    }

    @Test
    public void executesActionOnEachInterval() {

        // given
        final AtomicReference<EmitterLoop> loopHandle = new AtomicReference<>();
        final Runnable action = spy(new Runnable() {
            @Override
            public void run() {
                loopHandle.get().reset();
            }
        });

        final EmitterLoop emitterLoop = new EmitterLoop(10, action);
        loopHandle.set(emitterLoop);

        // when
        final Thread thread = new Thread(emitterLoop);
        thread.start();

        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(200));

        // then
        verify(action, atLeast(1)).run();
        verify(action, atMost(20)).run();

        emitterLoop.stop();

    }

    @Test
    public void executesActionOnWakeup() {

        // given
        final AtomicReference<EmitterLoop> loopHandle = new AtomicReference<>();
        final Runnable action = spy(new Runnable() {
            @Override
            public void run() {
                loopHandle.get().reset();
            }
        });

        final EmitterLoop emitterLoop = new EmitterLoop(10000, action);
        loopHandle.set(emitterLoop);

        final Thread thread = new Thread(emitterLoop);
        thread.start();

        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));

        verify(action, never()).run();

        // when
        emitterLoop.poke();

        // then
        verify(action, timeout(100)).run();

        emitterLoop.stop();

    }

    @Test
    public void emitterLoopRunRunsOnlyOnce() {

        // given
        final AtomicReference<EmitterLoop> loopHandle = new AtomicReference<>();
        final Runnable action = spy(new Runnable() {
            @Override
            public void run() {
                loopHandle.get().reset();
            }
        });

        final EmitterLoop emitterLoop = new EmitterLoop(10000, action);
        loopHandle.set(emitterLoop);

        Logger logger = mockTestLogger();

        // when
        final Thread thread = new Thread(emitterLoop);
        thread.start();

        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));

        emitterLoop.run(); // that's the 'fly-through' - exiting immediately

        verify(logger, never()).info(eq("{}: Ignoring wakeup while not running"), eq(AsyncBatchEmitter.EmitterLoop.class.getSimpleName()));

        emitterLoop.stop(); // unlatch and hit the logger

        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));

        // then
        verify(logger).info(eq("{}: Ignoring wakeup while not running"), eq(AsyncBatchEmitter.EmitterLoop.class.getSimpleName()));

    }

    @Test
    public void emitterLoopStopsOnlyOnce() {

        // given
        final AtomicReference<EmitterLoop> loopHandle = new AtomicReference<>();
        final Runnable action = spy(new Runnable() {
            @Override
            public void run() {
                loopHandle.get().reset();
            }
        });

        final EmitterLoop emitterLoop = new EmitterLoop(10000, action);
        loopHandle.set(emitterLoop);

        Logger logger = mockTestLogger();

        // when
        final Thread thread = new Thread(emitterLoop);
        thread.start();

        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));

        emitterLoop.stop(); // unlatch and hit the logger
        emitterLoop.stop(); // no effect

        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));

        // then
        verify(logger).info(eq("{}: Loop stopped"), eq(AsyncBatchEmitter.EmitterLoop.class.getSimpleName()));
        verify(logger).info(eq("{}: Ignoring wakeup while not running"), eq(AsyncBatchEmitter.EmitterLoop.class.getSimpleName()));

    }

    @Test
    public void interruptedExceptionOnEmitterLoopIsHandled() {

        // given
        final Logger logger = mockTestLogger();
        final Runnable action = new Runnable() {
            @Override
            public void run() {
                logger.info("Action executed");
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10000));
            }
        };

        final AsyncBatchEmitter.EmitterLoop emitterLoop = new EmitterLoop(1000, action);


        Thread t1 = new Thread(emitterLoop);

        // when
        t1.start();
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));
        emitterLoop.poke();
        verify(logger, timeout(500)).debug(eq("{}: Executing on {}"), eq(AsyncBatchEmitter.EmitterLoop.class.getSimpleName()), eq("demand"));
        t1.interrupt();

        // then
        verify(logger, timeout(500)).error(eq("{}: Loop interrupted. Stopping"), eq(AsyncBatchEmitter.EmitterLoop.class.getSimpleName()));
        verify(logger, timeout(500)).info(eq("{}: Stopped"), eq(AsyncBatchEmitter.EmitterLoop.class.getSimpleName()));

    }

    @Test
    public void executionExceptionOnEmitterLoopIsHandled() {

        // given
        final Logger logger = mockTestLogger();

        final AtomicReference<EmitterLoop> loopHandle = new AtomicReference<>();
        final String exceptionMessage = UUID.randomUUID().toString();

        final Runnable action = () -> {
            loopHandle.get().reset();
            throw new RuntimeException(exceptionMessage);
        };

        final EmitterLoop emitterLoop = new EmitterLoop(1000, action);
        loopHandle.set(emitterLoop);

        Thread t1 = new Thread(emitterLoop);

        // when
        t1.start();
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));
        emitterLoop.poke();

        // then
        verify(logger, timeout(500)).error(eq("{}: Execution failed: {}"), eq(AsyncBatchEmitter.EmitterLoop.class.getSimpleName()), eq(exceptionMessage));
        verify(logger, times(1)).debug(eq("{}: Executing on {}"), eq(AsyncBatchEmitter.EmitterLoop.class.getSimpleName()), eq("demand"));

    }

}
