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

public interface LifeCycle {

    enum State {
        STARTED, STOPPED
    }

    /**
     * Returns LifeCycle of given object
     *
     * @param o object to be checked for LifeCycle presence
     * @return LifeCycle of given object, {@link LifeCycle#NOOP} otherwise
     */
    static LifeCycle of(Object o) {
        return o instanceof LifeCycle ? (LifeCycle) o : NOOP;
    }

    LifeCycle NOOP = new Noop();

    void start();

    /**
     * Delegates to {@link #stop(long, boolean)} by default
     */
    default void stop() {
        stop(0, false);
    }

    default LifeCycle stop(long timeout, boolean runInBackground) {
        return this;
    }

    /**
     * Starts extensions.
     *
     * Allows to encapsulate state in base classes.
     *
     * SHOULD be invoked BEFORE {@link State} changes to {@link State#STARTED}
     */
    default void startExtensions() {

    };

    /**
     * Stops extensions.
     *
     * Allows to encapsulate state in base classes.
     *
     * SHOULD be invoked BEFORE {@link State} changes to {@link State#STOPPED}
     */
    default void stopExtensions() {

    };

    boolean isStarted();

    boolean isStopped();

    class Noop implements LifeCycle {

        @Override
        public void start() {

        }

        @Override
        public boolean isStarted() {
            return false;
        }

        @Override
        public boolean isStopped() {
            return false;
        }

    }

}
