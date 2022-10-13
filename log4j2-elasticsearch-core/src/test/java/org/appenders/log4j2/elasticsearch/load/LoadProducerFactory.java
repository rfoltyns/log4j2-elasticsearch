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

import java.util.function.Supplier;

public class LoadProducerFactory {

    private final LoadTask task;
    private final Supplier<Boolean> condition;

    public LoadProducerFactory(final LoadTask task, final Supplier<Boolean> condition) {
        this.task = task;
        this.condition = condition;
    }

    public ConditionalLoop createProducer(final boolean startOnCreate, final Supplier<Integer> sizeSupplier, final Supplier<Integer> sleepMillisSupplier) {

        final Runnable generateLoad = () -> task.generateLoad(sizeSupplier.get(), sleepMillisSupplier.get());
        final ConditionalLoop loop = new ConditionalLoop(generateLoad, condition);

        if (startOnCreate) {
            loop.start();
        }

        return loop;

    }

}
