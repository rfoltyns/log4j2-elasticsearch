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
import org.jctools.queues.SpscUnboundedArrayQueue;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static final Map<String, QueueFactory> CACHED_INSTANCES = new HashMap<>();

    private final Features features;
    private final Factory spsc;
    private final Factory mpsc;
    private final Factory mpmc;

    // visible for testing
    QueueFactory(final String name) {
        this.features = Features.builder()
                .configure(Features.Feature.JCTOOLS_QUEUES,
                        Boolean.parseBoolean(System.getProperty(String.format("appenders.%s.%s.enabled", name, "jctools"), Boolean.toString(Features.Feature.JCTOOLS_QUEUES.isEnabled()))))
                .build();

        spsc = resolveFactory(name, "org.jctools.queues.SpscUnboundedArrayQueue", SpscUnboundedArrayQueue::new);
        mpsc = resolveFactory(name, "org.jctools.queues.MpscUnboundedArrayQueue", MpscUnboundedArrayQueue::new);
        mpmc = resolveFactory(name, "org.jctools.queues.MpmcUnboundedXaddArrayQueue", MpmcUnboundedXaddArrayQueue::new);
    }

    public static QueueFactory getQueueFactoryInstance(final String name) {
        return CACHED_INSTANCES.computeIfAbsent(name, QueueFactory::new);
    }

    public final <T> Queue<T> tryCreateMpscQueue(final int initialSize) {
        //noinspection unchecked
        return mpsc.create(initialSize);
    }

    public final <T> Queue<T> tryCreateSpscQueue(final int initialSize) {
        //noinspection unchecked
        return spsc.create(initialSize);
    }

    public final <T> Queue<T> tryCreateMpmcQueue(final int initialSize) {
        //noinspection unchecked
        return mpmc.create(initialSize);
    }

    private Factory resolveFactory(final String name, final String queueClassName, final Factory preferredFactory) {

        if (features.isEnabled(Features.Feature.JCTOOLS_QUEUES)) {
            if (hasClass(name, queueClassName)) {
                getLogger().debug("{}: Using {}", name, queueClassName);
                return preferredFactory;
            }
        }

        getLogger().debug("{}: Falling back to {}",
                name,
                ConcurrentLinkedQueue.class.getName());

        return size -> new ConcurrentLinkedQueue<>();
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

    private static class Features {

        private final BitSet state;

        public Features(BitSet state) {
            this.state = state;
        }

        public final boolean isEnabled(final Feature feature) {
            return this.state.get(feature.ordinal());
        }

        public static Builder builder() {
            return new Builder();
        }

        private static class Builder {

            private final BitSet state = new BitSet();

            Builder configure(final Feature feature, final boolean enabled) {
                state.set(feature.ordinal(), enabled);
                return this;
            }

            public Features build() {
                return new Features(state);
            }
        }

        private enum Feature {

            JCTOOLS_QUEUES(true);

            private final boolean enabled;

            Feature(final boolean enabled) {
                this.enabled = enabled;
            }

            public final boolean isEnabled() {
                return enabled;
            }

        }
    }

    private interface Factory<T> {
        Queue<T> create(final int size);
    }

}
