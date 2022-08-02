package org.appenders.log4j2.elasticsearch.jmh;

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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.apache.logging.log4j.LogManager;
import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.metrics.BasicMetricsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.MaxLongMetric;
import org.appenders.log4j2.elasticsearch.metrics.Metric;
import org.appenders.log4j2.elasticsearch.metrics.MetricOutput;
import org.appenders.log4j2.elasticsearch.metrics.MetricsProcessor;
import org.appenders.log4j2.elasticsearch.smoke.Log4j2Delegate;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Fork(jvmArgsAppend = {
        "-ea",
        "-Xmx2g",
        "-Xms2g",
        "-XX:+AlwaysPreTouch",
        "-Djmh.pinned=true",
}
)
public class MetricsProcessorTest {

    private final AtomicInteger collectCallCount = new AtomicInteger();

    private MetricsProcessor metricsProcessor;

    int numberOfMetrics = 10;
    private Logger logger;

    @Setup
    public void prepare() {

        final BasicMetricsRegistry metricRegistry = new BasicMetricsRegistry();

        this.metricsProcessor = new MetricsProcessor(metricRegistry, new MetricOutput[] {
                new MetricDummy(),
        });

        for (int i = 0; i < numberOfMetrics; i++) {
            final Metric metric = new MaxLongMetric(new Metric.Key("test-component", UUID.randomUUID().toString(), "test"), 0L, false);
            metricRegistry.register(metric);
        }

        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));

    }

    private Logger getLogger() {
        if (logger == null) {
            logger = new Log4j2Delegate(LogManager.getLogger(MetricsProcessorTest.class));
//            logger = new Log4j2Delegate(StatusLogger.getLogger());
        }
        return logger;
    }

    @Benchmark
    public void processMetrics(final Blackhole fox) {
        metricsProcessor.process();
    }

    @TearDown
    public void tearDown() {
        System.out.println("Call count: " + collectCallCount.getAndSet(0));
    }

    private class MetricDummy implements MetricOutput {

        private ByteBuf byteBuf = new CompositeByteBuf(UnpooledByteBufAllocator.DEFAULT, false, 2);

        @Override
        public boolean accepts(final Metric.Key key) {
            return true;
        }

        @Override
        public void write(final long timestamp, final Metric.Key key, long value) {
            getLogger().info("{} {}: {}={}", timestamp, key, key, value);
        }

        @Override
        public void flush() {

        }

    }

}
