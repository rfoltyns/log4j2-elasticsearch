package org.appenders.log4j2.elasticsearch;

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

import org.jctools.queues.MpmcUnboundedXaddArrayQueue;
import org.jctools.queues.MpscUnboundedArrayQueue;
import org.jctools.queues.SpscUnboundedArrayQueue;

public class JCToolsFactory {

    public <T> QueueFactory.Factory<T> create(final String queueClassName) {

        if (queueClassName.endsWith("MpmcUnboundedXaddArrayQueue")) {
            return MpmcUnboundedXaddArrayQueue::new;
        }
        if (queueClassName.endsWith("MpscUnboundedArrayQueue")) {
            return MpscUnboundedArrayQueue::new;
        }
        if (queueClassName.endsWith("SpscUnboundedArrayQueue")) {
            return SpscUnboundedArrayQueue::new;
        }

        throw new IllegalArgumentException("Class not supported: " + queueClassName);

    }

}
