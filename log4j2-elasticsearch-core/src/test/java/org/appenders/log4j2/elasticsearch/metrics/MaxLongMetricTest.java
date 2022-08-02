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

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MaxLongMetricTest {

    static final Metric.Key TEST_METRIC_KEY = new Metric.Key("test-component", "test-metric", "test");
    private final Random random = new Random();

    @Test
    public void defaultInitialValueIsZero() {

        // given
        final Metric metric = new MaxLongMetric(TEST_METRIC_KEY, false);
        metric.store(-1);

        // when
        long result = metric.getValue();

        // then
        assertEquals(0L, result);

    }

    @Test
    public void storesLong() {

        // given
        final Metric metric = new MaxLongMetric(TEST_METRIC_KEY, Long.MIN_VALUE, false);

        final long expected = random.nextLong();

        // when
        metric.store(expected);

        // then
        assertEquals(expected, metric.getValue());

    }

    @Test
    public void storesInt() {

        // given
        final Metric metric = new MaxLongMetric(TEST_METRIC_KEY, Long.MIN_VALUE, false);

        final int expected = random.nextInt();

        // when
        metric.store(expected);

        // then
        assertEquals(expected, metric.getValue());

    }

    @Test
    public void storesMaxLong() {

        // given
        final Metric metric = new MaxLongMetric(TEST_METRIC_KEY, Long.MIN_VALUE, false);

        final long expectedLong1 = random.nextLong();
        final long expectedLong2 = random.nextLong();

        metric.store(expectedLong1);
        assertEquals(expectedLong1, metric.getValue());
        metric.store(expectedLong2);

        final long expected = Math.max(expectedLong1, expectedLong2);

        // when
        metric.store(expected);

        // then
        assertEquals(expected, metric.getValue());

    }

    @Test
    public void storesMaxInt() {

        // given
        final Metric metric = new MaxLongMetric(TEST_METRIC_KEY, Long.MIN_VALUE, false);

        final long expectedInt1 = random.nextInt();
        final long expectedInt2 = random.nextInt();

        metric.store(expectedInt1);
        assertEquals(expectedInt1, metric.getValue());
        metric.store(expectedInt2);

        final long expected = Math.max(expectedInt1, expectedInt2);

        // when
        metric.store(expected);

        // then
        assertEquals(expected, metric.getValue());

    }

    @Test
    public void allStoreMethodsShareTheSameInternalStore() {

        // given
        final Metric metric = new MaxLongMetric(TEST_METRIC_KEY, Long.MIN_VALUE, false);
        final int expectedInt = random.nextInt();
        final long expectedLong = random.nextLong();

        metric.store(expectedInt);
        assertEquals(expectedInt, metric.getValue());
        metric.store(expectedLong);

        final long expected = Math.max(expectedLong, expectedInt);

        // when
        final long result = metric.getValue();

        // then
        assertEquals(expected, result);

    }

    @Test
    public void resetReturnsAccumulatedValue() {

        // given
        final Metric metric = new MaxLongMetric(TEST_METRIC_KEY, Long.MIN_VALUE, false);
        final long unexpected = random.nextLong();
        final long expected = unexpected + 1L;

        metric.store(unexpected);
        assertEquals(unexpected, metric.getValue());
        metric.store(expected);

        // when
        final long result = metric.reset();

        // then
        assertEquals(Long.MIN_VALUE, metric.getValue());
        assertEquals(expected, result);

    }

    @Test
    public void consumerResetsValueIfConfigured() {

        // given
        final Metric metric = new MaxLongMetric(TEST_METRIC_KEY, Long.MIN_VALUE, true);
        final long unexpected = random.nextLong();
        final long expected = unexpected + 1L;

        metric.store(unexpected);
        assertEquals(unexpected, metric.getValue());
        metric.store(expected);

        // when
        metric.accept((name, value) -> assertEquals(expected, value));

        // then
        assertEquals(Long.MIN_VALUE, metric.getValue());

    }

    @Test
    public void consumerRetainsValueIfConfigured() {

        // given
        final Metric metric = new MaxLongMetric(TEST_METRIC_KEY, Long.MIN_VALUE, false);
        final long unexpected = random.nextLong();
        final long expected = unexpected + 1L;

        metric.store(unexpected);
        assertEquals(unexpected, metric.getValue());
        metric.store(expected);

        // when
        metric.accept((name, value) -> assertEquals(expected, value));

        // then
        assertEquals(expected, metric.getValue());

    }

}
