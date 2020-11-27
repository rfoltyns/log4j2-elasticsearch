package org.appenders.log4j2.elasticsearch;

import net.openhft.chronicle.core.util.Ints;
import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.AsyncBatchEmitter.EmitterLoop;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;

import static org.appenders.core.logging.InternalLoggingTest.mockTestLogger;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class EmitterLoopTest {

    @AfterEach
    public void tearDown() {
        InternalLogging.setLogger(null);
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

        verify(action, never()).run();

        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));

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

}