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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

import java.util.function.Consumer;

/**
 * Allows to define multi-phase shutdown procedures. Currently, 3 phases can be defined:
 * <ul>
 * <li>{@link #onStart} is run immediately after shutdown is started
 * Suitable for actual shutdown task or preparation for shutdown, e.g.: shutting down thread pools.
 * <li>{@link #onDecrement(Consumer)} is run after {@link #onStart} each {@link #decrementInMillis}
 * while {@code delay - timeTakenToExecuteOnStart - decrementInMillis * numberOfOnDecrementExecutions > 0}
 * Suitable for shutdown monitoring.
 * <li>{@link #afterDelay(Runnable)} is run after defined {@link #delay(long)}
 * Suitable for actual shutdown task, cleanup tasks, etc.
 * </ul>
 * <p>{@link #onError(Consumer)} can be specified to handle any {@link #onStart} and {@link #onDecrement} runtime errors.
 * Once executed, {@link #afterDelay(Runnable)} will be executed immediately regardless of remaining delay.
 * <p>
 * {@link #onError(Consumer)} will NOT be executed after {@link #afterDelay(Runnable)} errors.
 * <p>If {@link #onStart} takes more than {@link #delay}, it will continue until completion.
 * In this case {@link #onDecrement(Consumer)} will never be executed
 * and shutdown will proceed to {@link #afterDelay(Runnable)} immediately
 * 
 */
public class DelayedShutdown extends Thread {

    private static final Logger LOG = StatusLogger.getLogger();

    /**
     * Default: 1000
     */
    public static final int DEFAULT_DECREMENT_IN_MILLIS = 1000;

    private final Runnable onStart;

    private long delay = 0;
    private long decrementInMillis = DEFAULT_DECREMENT_IN_MILLIS;
    private Consumer<Long> onDecrement = remaining -> {};
    private Runnable afterDelay = () -> {};
    private Consumer<Exception> onError = exception -> LOG.warn("Shutdown interrupted: {}", exception.getMessage());

    /**
     * @param onStart Task to execute immediately after start
     */
    public DelayedShutdown(Runnable onStart) {
        super("DelayedShutdown");
        this.onStart = onStart;
    }

    /**
     * Runs when exception occurs
     *
     * @param onError {@code java.lang.Exception} consumer
     * @return this
     */
    public DelayedShutdown onError(Consumer<Exception> onError) {
        this.onError = onError;
        return this;
    }

    /**
     * Runs after {@link #delay} has passed or {@link #onStart} has finished, whichever happens later
     *
     * @param afterDelay Task to execute
     * @return this
     */
    public DelayedShutdown afterDelay(Runnable afterDelay) {
        this.afterDelay = afterDelay;
        return this;
    }

    /**
     * Runs after {@link #onStart} on every {@link #decrementInMillis} if {@link #delay} has not been reached yet
     *
     * @param onDecrement Task to execute. Accepts remaining millis.
     * @return this
     */
    public DelayedShutdown onDecrement(Consumer<Long> onDecrement) {
        this.onDecrement = onDecrement;
        return this;
    }

    /**
     * Millis before {@link #afterDelay(Runnable)} is executed.
     * If {@link #onStart} takes longer than given delay, delay will be ignored.
     *
     * @param delayInMillis minimum delay between shutdown start and {@link #afterDelay(Runnable)}
     * @return this
     */
    public DelayedShutdown delay(long delayInMillis) {
        this.delay = delayInMillis;
        return this;
    }

    /**
     * Milliseconds between each {@link #onDecrement(Consumer)} execution if remaining delay is higher than 0
     *
     * @param decrementInMillis number of milliseconds to be deducted from remaining delay after {@link #onStart} is finished
     * @return this
     */
    public DelayedShutdown decrementInMillis(int decrementInMillis) {
        this.decrementInMillis = decrementInMillis;
        return this;
    }

    /**
     * Starts synchronous execution
     */
    @Override
    public final void run() {

        try {

            long start = System.currentTimeMillis();

            onStart.run();

            long took = System.currentTimeMillis() - start;
            long remaining = delay - took;

            while (remaining > 0) {
                onDecrement.accept(remaining);
                sleep(decrementInMillis);
                remaining -= decrementInMillis;
            }

        } catch (InterruptedException e) {
            onError.accept(e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            onError.accept(e);
        } finally {
            afterDelay.run();
        }

    }

    /**
     * Allows to start execution in background
     *
     * @param runInBackground If <i>true</i>, execution will be run in background, sync execution otherwise
     */
    public final void start(boolean runInBackground) {
        if (runInBackground) {
            start();
            return;
        }
        run();
    }

}
