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

import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MetricOutputTest {

    public static MetricOutput dummy() {
        return new DummyMetricOutput();
    }

    @Test
    public void comparesWithStringComparatorByDefault() {

        // given
        final MetricOutput output1 = new DummyMetricOutput("aaa");
        final MetricOutput output2 = new DummyMetricOutput("aab");
        final MetricOutput output3 = new DummyMetricOutput("bbb");
        final MetricOutput output4 = new DummyMetricOutput("Aa");
        final MetricOutput output5 = new DummyMetricOutput("Ab");
        final MetricOutput output6 = new DummyMetricOutput("Bbb");
        final MetricOutput output7 = new DummyMetricOutput("1");
        final MetricOutput output8 = new DummyMetricOutput("2");
        final MetricOutput output9 = new DummyMetricOutput("2");

        final BasicMetricOutputsRegistry manager = new BasicMetricOutputsRegistry(output1, output2, output3);

        // when
        final int result1 = output1.compareTo(output2);
        final int result2 = output2.compareTo(output3);
        final int result3 = output3.compareTo(output4);
        final int result4 = output4.compareTo(output5);
        final int result5 = output5.compareTo(output6);
        final int result6 = output6.compareTo(output7);
        final int result7 = output7.compareTo(output8);
        final int result8 = output8.compareTo(output9);

        manager.register(output1);
        manager.register(output2);
        manager.register(output3);
        manager.register(output4);
        manager.register(output5);
        manager.register(output6);
        manager.register(output7);
        manager.register(output8);
        manager.register(output9);

        final Iterator<MetricOutput> metricOutputs = manager.get(o -> true).iterator();

        // then
        assertEquals(-1, result1);
        assertEquals(-1, result2);
        assertEquals(1, result3);
        assertEquals(-1, result4);
        assertEquals(-1, result5);
        assertEquals(1, result6);
        assertEquals(-1, result7);
        assertEquals(0, result8);

        assertEquals(output7, metricOutputs.next());
        assertEquals(output9, metricOutputs.next());
        assertEquals(output4, metricOutputs.next());
        assertEquals(output5, metricOutputs.next());
        assertEquals(output6, metricOutputs.next());
        assertEquals(output1, metricOutputs.next());
        assertEquals(output2, metricOutputs.next());
        assertEquals(output3, metricOutputs.next());

    }

    static class DummyMetricOutput implements MetricOutput, LifeCycle {

        private final String name;

        DummyMetricOutput() {
            name = UUID.randomUUID().toString();
        }

        DummyMetricOutput(final String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean accepts(Metric.Key key) {
            return true;
        }

        @Override
        public void write(long timestamp, Metric.Key key, long value) {

        }

        @Override
        public void flush() {

        }

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
