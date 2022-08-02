package org.appenders.log4j2.elasticsearch.metrics;

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

import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * Collects metrics provided by given {@link MetricsRegistry} and writes collected values to compatible {@link MetricOutput}(s).
 * <p>
 * Processes all metrics registered with {@link MetricsRegistry} before creation of instance this class.
 * <p>
 * Processes all metrics registered with {@link MetricsRegistry} after creation of instance this class.
 */
public class MetricsProcessor implements Measured {

    private final Tasks tasks;
    private final MetricsRegistry registry;

    /**
     * @param registry registered metrics store
     * @param metricOutputs collected values listeners
     */
    public MetricsProcessor(final MetricsRegistry registry, final MetricOutput[] metricOutputs) {
        this(Clock.systemDefaultZone(), registry, metricOutputs);
    }

    /**
     * @param clock clock to use on value collection
     * @param registry registered metrics store
     * @param metricOutputs collected values listeners
     */
    public MetricsProcessor(final Clock clock,
                            final MetricsRegistry registry,
                            final MetricOutput[] metricOutputs) {
        this.tasks = new Tasks();
        this.registry = registry;
        tasks.set(new Reconfigure(clock, this.registry, tasks, metricOutputs).createTaskList());
    }

    /**
     * Collects metrics registered with {@link MetricsRegistry} and delivers collected values to {@link MetricOutput}s.
     * <p>
     * Timestamp is taken at the start of each call and used for all collected values regardless of individual metric collection latency.
     * Metrics stored between timestamp calculation and actual collection are considered a part of current collection.
     * In cases where metric collection takes more than the smallest time unit supported by used clock, metric data stored after timestamp calculation will "leak" into the current snapshot, but this case is considered extreme.
     * In most cases metric collection latency is negligible and this trade-off SHOULD be considered acceptable.
     */
    public final void process() {
        tasks.run();
    }

    @Override
    public final void register(final MetricsRegistry registry) {
        // no metrics yet
    }

    @Override
    public void register(final Measured measured) {
        measured.register(registry);
    }

    /**
     * Removes all registered metrics from {@link MetricsRegistry}. Next {@link #process()} will not collect any metrics or write to any {@link MetricOutput}s.
     *
     * In practice, may be used to dereference dangling metrics that failed to {@link Measured#deregister()} on their own.
     */
    public void reset() {
        registry.clear();
    }


    private static class Collector implements MetricCollector {

        private final Clock clock;

        private final Itr<CollectedValue> iterator;

        private long lastCollectionTimestamp;

        private Collector(final Clock clock, final Set<Metric> metrics) {
            this.clock = clock;
            this.iterator = new Itr<>(metrics.stream()
                    .map(Metric::getKey)
                    .map(CollectedValue::new)
                    .toArray(CollectedValue[]::new));
        }

        void stamp() {
            lastCollectionTimestamp = clock.millis();
        }

        @Override
        public void collect(final Metric.Key key, final long value) {
            iterator.next().setValue(value);
        }

    }

    private static class CollectedValue {

        private final Metric.Key key;

        private volatile long value;

        private CollectedValue(final Metric.Key key) {
            this.key = key;
        }

        public Metric.Key getKey() {
            return key;
        }

        public long getValue() {
            return value;
        }

        public void setValue(final long value) {
            this.value = value;
        }

    }

    private static class Itr<T> implements Iterator<T> {

        private final T[] vh;

        private int current = 0;

        private Itr(final T[] valueHolders) {
            this.vh = valueHolders;
        }

        @Override
        public boolean hasNext() {
            return current < vh.length;
        }

        @Override
        public T next() {
            return vh[current++ % vh.length];
        }

        public void reset() {
            this.current = 0;
        }

    }

    private interface Task {

        int execute();

    }

    private static class Writer {

        private final MetricOutput output;

        private final CollectedValue[] collectedValues;
        public Writer(final MetricOutput output, final CollectedValue[] collectedValues) {
            this.output = output;
            this.collectedValues = collectedValues;
        }

        public void flush(final long timestamp) {

            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < collectedValues.length; i++) {
                final CollectedValue collectedValue = collectedValues[i];
                output.write(timestamp, collectedValue.getKey(), collectedValue.getValue());
            }

            output.flush();

        }

    }

    private static class Tasks implements Runnable {

        private final AtomicReference<Task[]> tasks;

        public Tasks() {
            this.tasks = new AtomicReference<>(new Task[0]);
        }

        @Override
        public void run() {

            final Task[] tasks = this.tasks.get();
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < tasks.length; i++) {
                if (tasks[i].execute() == Reconfigure.RECONFIGURED) {
                    run(); // run new set of tasks once reconfigured immediately
                    break; // bail as tasks in this scope are not valid anymore
                }
            }

        }

        public void set(final Task[] tasks) {
            this.tasks.set(tasks);
        }

    }

    private static class Reconfigure implements Task {

        public static final Predicate<Metric> EXCLUDE_NOOP = metric -> !"noop".equals(metric.getKey().getMetricTypePart());
        private static final int NO_CHANGES = 0;
        private static final int RECONFIGURED = 1;

        private final MetricOutput[] writers;
        private final MetricsRegistry registry;
        private final Tasks tasks;
        private final AtomicLong lastKnownVersion;
        private final Clock clock;

        private Reconfigure(final Clock clock, final MetricsRegistry registry, final Tasks tasks, final MetricOutput[] writers) {
            this.clock = clock;
            this.lastKnownVersion = new AtomicLong(registry.version());
            this.registry = registry;
            this.tasks = tasks;
            this.writers = writers;
        }

        @Override
        public int execute() {

            final long version = registry.version();

            final long lastKnownVersion = this.lastKnownVersion.get();
            if (lastKnownVersion < version) {
                this.lastKnownVersion.set(version);

                final Task[] newTasks = createTaskList();
                tasks.set(newTasks);

                return RECONFIGURED;

            }

            return NO_CHANGES;

        }

        @NotNull
        private Task[] createTaskList() {

            final Set<Metric> metrics = registry.getMetrics(EXCLUDE_NOOP);
            final Collector collector = new Collector(clock, metrics);

            return new Task[] {
                    new Reconfigure(clock, registry, tasks, writers),
                    new Collect(collector, metrics),
                    new Flush(collector, writers)
            };

        }

    }

    private static class Collect implements Task {

        private final Collector collector;
        private final Metric[] collectables;

        private Collect(final Collector collector, final Set<Metric> collectables) {
            this.collector = collector;
            this.collectables = collectables.toArray(new Metric[0]);
        }

        @Override
        public int execute() {

            collector.stamp();

            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < collectables.length; i++) {
                final Metric metric = collectables[i];
                metric.accept(collector);
            }

            return 0;

        }

    }

    private static class Flush implements Task {

        private final Collector collector;
        private final Writer[] writers;

        private Flush(final Collector collector, final MetricOutput[] writers) {

            this.collector = collector;

            final Writer[] internalWriters = new Writer[writers.length];
            for (int i = 0; i < writers.length; i++) {

                final MetricOutput writer = writers[i];

                final Writer internalWriter = getInternalWriter(collector, writer);
                internalWriters[i] = internalWriter;
            }

            this.writers = internalWriters;

        }

        private Writer getInternalWriter(final Collector collector, final MetricOutput writer) {

            final List<CollectedValue> collectedValues = new ArrayList<>();

            final Itr<CollectedValue> iterator = collector.iterator;
            while (iterator.hasNext()) {

                final CollectedValue next = iterator.next();
                if (writer.accepts(next.getKey())) {
                    collectedValues.add(next);
                }

            }

            iterator.reset();

            return new Writer(writer, collectedValues.toArray(new CollectedValue[0]));

        }

        @Override
        public int execute() {

            final long timestamp = collector.lastCollectionTimestamp;

            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < writers.length; i++) {
                final Writer writer = writers[i];
                writer.flush(timestamp);
            }

            return 0;

        }

    }

}
