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

import org.jctools.queues.MpmcUnboundedXaddArrayQueue;
import org.jctools.queues.MpscUnboundedArrayQueue;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * <p>
 * Helper class that allows to create <a href="https://github.com/JCTools/JCTools">JCTools</a> queues.
 * </p>
 * <p>
 * JCTools queues will be created by default if they're found in the Thread context classloader and creation is not disabled.
 * </p>
 * <p>
 * Creation of JCTools queues can be disabled per queue name. Once disabled, methods will fallback to {@code java.util.concurrent.ConcurrentLinkedQueue}.
 * E.g. setting {@code -Dorg.appenders.GenericItemSourcePool.jctools.enabled=false} will get this class to create a fallback queue
 * </p>
 * NOTE: Consider this class <i>private</i>. API is highly experimental and can change without notice
 */
public class QueueFactory {

    private static final QueueFactory INSTANCE = new QueueFactory();

    public static QueueFactory getQueueFactoryInstance() {
        return INSTANCE;
    }

    public final <T> Queue<T> tryCreateMpscQueue(final String name, final int initialSize) {
        return tryCreate(name, "org.jctools.queues.MpscUnboundedArrayQueue", initialSize);
    }

    public final <T> Queue<T> tryCreateMpmcQueue(final String name, final int initialSize) {
        return tryCreate(name, "org.jctools.queues.MpmcUnboundedXaddArrayQueue", initialSize);
    }

    final <T> Queue<T> tryCreate(final String name, final String queueClassName, final int initialSize) {

        if (isEnabled(name, "jctools") && hasClass(name, queueClassName)) {
            getLogger().debug("{}: Using {}", name, queueClassName);
            switch (queueClassName) {
                case "org.jctools.queues.MpmcUnboundedXaddArrayQueue":
                    return new MpmcUnboundedXaddArrayQueue<>(initialSize);
                case "org.jctools.queues.MpscUnboundedArrayQueue":
                    return new MpscUnboundedArrayQueue<>(initialSize);
                default:
                    throw new UnsupportedOperationException(queueClassName + " is not supported");
            }
        }

        final ConcurrentLinkedQueue<T> fallback = new ConcurrentLinkedQueue<>();

        getLogger().debug("{}: Falling back to {}",
                name,
                fallback.getClass().getName());

        return fallback;
    }

    private boolean isEnabled(final String name, final String featureName) {
        final String propertyName = String.format("appenders.%s.%s.enabled", name, featureName);
        return Boolean.parseBoolean(System.getProperty(propertyName, "true"));
    }

    /* visible for testing */
    boolean hasClass(final String name, final String className) {

        try {
            Thread.currentThread().getContextClassLoader().loadClass(className);
            return true;
        } catch (ClassNotFoundException e) {
            getLogger().debug("{}: {} not available",
                    name,
                    className);
            return false;
        }

    }

    /**
     * Helper method that converts non-iterable queues to {@code ArrayList} if needed.
     *
     * @param items Collections to convert
     * @param <T> item type
     * @return new iterable collection or {@code items}
     */
    public <T> Collection<T> toIterable(final Collection<T> items) {

        // CLQ supports iterator(), so nothing to do
        if (items instanceof ConcurrentLinkedQueue) {
            return items;
        }

        if (items instanceof AbstractQueue) {
            AbstractQueue<T> arrayQueue = (AbstractQueue<T>)items;

            int size = items.size();
            // Unlike CLQ, ArrayList doesn't allocate on add()
            List<T> result = new ArrayList<>(size);
            while (size-- > 0) {
                result.add(arrayQueue.poll());
            }

            return result;
        }

        return items;

    }

}
