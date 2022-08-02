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

public class CountMetricTest {

    static final Metric.Key TEST_METRIC_KEY = new Metric.Key("test-component", "test-metric", "test");

    private final Random random = new Random();

    @Test
    public void storesLong() {

        // given
        final Metric metric = new CountMetric(TEST_METRIC_KEY);

        final long expected = random.nextLong();

        // when
        metric.store(expected);

        // then
        assertEquals(expected, metric.getValue());

    }

    @Test
    public void storesInt() {

        // given
        final Metric metric = new CountMetric(TEST_METRIC_KEY);

        final int expected = random.nextInt();

        // when
        metric.store(expected);

        // then
        assertEquals(expected, metric.getValue());

    }

    @Test
    public void allStoreMethodsShareTheSameInternalStore() {

        // given
        final Metric metric = new CountMetric(TEST_METRIC_KEY);
        final long expectedLong = random.nextLong();
        final int expectedInt = random.nextInt();

        final long expected = expectedLong + expectedInt;

        // when
        metric.store(expectedLong);
        metric.store(expectedInt);

        // then
        assertEquals(expected, metric.getValue());

    }

    @Test
    public void resetReturnsAccumulatedValue() {

        // given
        final int initialValue = random.nextInt();
        final long expectedLong = random.nextLong();
        final int expectedInt = random.nextInt();
        final Metric metric = new CountMetric(TEST_METRIC_KEY, initialValue, false);

        final long expected = initialValue + expectedLong + expectedInt;
        metric.store(expectedLong);
        metric.store(expectedInt);

        // when
        final long result = metric.reset();

        // then
        assertEquals(expected, result);

    }

    @Test
    public void resetsToInitialValue() {

        // given
        final int initialValue = random.nextInt();
        final long expectedLong = random.nextLong();
        final int expectedInt = random.nextInt();
        final Metric metric = new CountMetric(TEST_METRIC_KEY, initialValue, false);

        metric.store(expectedLong);
        metric.store(expectedInt);

        // when
        metric.reset();

        // then
        assertEquals(initialValue, metric.getValue());

    }

    @Test
    public void consumerResetsValueIfConfigured() {

        // given
        final int initialValue = random.nextInt();
        final long expectedLong = random.nextLong();
        final int expectedInt = random.nextInt();
        final Metric metric = new CountMetric(TEST_METRIC_KEY, initialValue, true);

        final long expected = initialValue + expectedLong + expectedInt;
        metric.store(expectedLong);
        metric.store(expectedInt);

        // when
        metric.accept((name, value) -> assertEquals(expected, value));

        // then
        assertEquals(initialValue, metric.getValue());

    }

    @Test
    public void consumerRetainsValueIfConfigured() {

        // given
        final int initialValue = random.nextInt();
        final long expectedLong = random.nextLong();
        final int expectedInt = random.nextInt();
        final Metric metric = new CountMetric(TEST_METRIC_KEY, initialValue, false);

        final long expected = initialValue + expectedLong + expectedInt;
        metric.store(expectedLong);
        metric.store(expectedInt);

        // when
        metric.accept((name, value) -> assertEquals(expected, value));

        // then
        assertEquals(expected, metric.getValue());

    }

}
