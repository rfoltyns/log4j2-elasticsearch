package org.appenders.log4j2.elasticsearch;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DelayedShutdownTest {

    @Test
    public void runsGivenTask() {

        // given
        Runnable task = mock(Runnable.class);
        DelayedShutdown shutdown = new DelayedShutdown(task);

        // when
        shutdown.start(false);

        // then
        verify(task).run();

    }

    @Test
    public void canRunTaskAsynchronously() {

        // given
        Runnable onStartTask = mock(Runnable.class);
        DelayedShutdown shutdown = new DelayedShutdown(onStartTask);

        // when
        shutdown.start(true);

        // then
        verify(onStartTask, times(0)).run();
        verify(onStartTask, timeout(5000).times(1)).run();

    }

    @Test
    public void delaysExecutionOfDelayedTask() {

        // given
        Runnable delayedTask = mock(Runnable.class);
        DelayedShutdown shutdown = createDefaultDelayedShutdown()
                .afterDelay(delayedTask)
                .delay(100);

        // when
        shutdown.start(true);

        // then
        verify(delayedTask, times(0)).run();
        verify(delayedTask, timeout(5000).times(1)).run();

    }

    @Test
    public void runsDelayedTask() {

        // given
        Runnable delayedTask = mock(Runnable.class);
        DelayedShutdown shutdown = createDefaultDelayedShutdown();

        shutdown.afterDelay(delayedTask);

        // when
        shutdown.start(false);

        // then
        verify(delayedTask).run();

    }

    @Test
    public void doesNotDelayTaskByDefault() {

        // given
        Consumer<Long> onDecrement = mock(Consumer.class);
        DelayedShutdown shutdown = createDefaultDelayedShutdown()
                .onDecrement(onDecrement);

        // when
        shutdown.start(false);

        // then
        verify(onDecrement, times(0)).accept(any());

    }

    @Test
    public void canRunIntermediateTasks() {

        // given
        Consumer<Long> onDecrement = mock(Consumer.class);
        DelayedShutdown shutdown = createDefaultDelayedShutdown()
                .decrementInMillis(10)
                .delay(10)
                .onDecrement(onDecrement);

        // when
        shutdown.start(false);

        // then
        verify(onDecrement, times(1)).accept(any());

    }

    @Test
    public void runsDelayedTaskWithNoDecrementIfOnStartTaskTookLongerThanDelay() {

        // given
        Runnable delayedTask = mock(Runnable.class);
        Consumer<Long> notExpectedTask = mock(Consumer.class);

        long delay = 10;
        Runnable longRunningTask = () -> justSleep(100);
        DelayedShutdown shutdown = new DelayedShutdown(longRunningTask)
                .delay(delay)
                .onDecrement(notExpectedTask)
                .afterDelay(delayedTask);

        // when
        shutdown.start(false);

        // then
        verify(notExpectedTask, times(0)).accept(any());
        verify(delayedTask, times(1)).run();

    }

    @Test
    public void handlesInterruptedExceptions()  {

        // given
        Consumer<Exception> onError = mock(Consumer.class);

        DelayedShutdown shutdown = createDefaultDelayedShutdown()
                .delay(1000)
                .onError(onError);
        shutdown.start(true);

        // when
        new Thread(() -> shutdown.interrupt()).run();

        // then
        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(onError, timeout(1000).times(1)).accept(captor.capture());
        assertTrue(captor.getValue() instanceof InterruptedException);

    }

    @Test
    public void handlesExceptions()  {

        // given
        Consumer<Exception> onError = mock(Consumer.class);

        String expectedMessage = UUID.randomUUID().toString();
        DelayedShutdown shutdown = new DelayedShutdown(() -> {throw new NullPointerException(expectedMessage);})
                .delay(1000)
                .onError(onError);

        // when
        shutdown.start(true);

        // then
        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(onError, timeout(1000).times(1)).accept(captor.capture());
        assertEquals(expectedMessage, captor.getValue().getMessage());

    }

    @Test
    public void exceptionsAreNotRethrown()  {

        // given
        String expectedMessage = UUID.randomUUID().toString();
        DelayedShutdown shutdown = new DelayedShutdown(() -> {throw new NullPointerException(expectedMessage);});

        // when
        shutdown.start(false);

    }

    // just to remove clutter in test code
    private void justSleep(int sleepMillis) {
        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    public DelayedShutdown createDefaultDelayedShutdown() {
        return new DelayedShutdown(() -> {});
    }

}
