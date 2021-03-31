package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2019 Rafal Foltynski
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

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        new Thread(shutdown::interrupt).start();

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
        assertDoesNotThrow(() -> shutdown.start(false));

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
