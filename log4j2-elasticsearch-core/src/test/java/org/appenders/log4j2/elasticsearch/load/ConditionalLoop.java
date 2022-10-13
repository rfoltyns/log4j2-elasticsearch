package org.appenders.log4j2.elasticsearch.load;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2022 Rafal Foltynski
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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class ConditionalLoop extends Thread {

    private final Runnable runnable;
    private final Supplier<Boolean> condition;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public ConditionalLoop(final Runnable runnable, final Supplier<Boolean> condition) {
        this.runnable = runnable;
        this.condition = condition;
    }

    @Override
    public void run() {

        while (running.get() && condition.get()) {
            runnable.run();
        }
        stopLoop();

    }

    public void stopLoop() {
        running.compareAndSet(true, false);
    }

}
