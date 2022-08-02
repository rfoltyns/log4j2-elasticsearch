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

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IncludeExcludeTest {


    @Test
    public void isNotCaseSensitive() {

        // given
        final Metric.Key metricName1 = new Metric.Key("test-component", "test-Metric", "max");
        final Metric.Key metricName2 = new Metric.Key("Test-Component", "test-Metric", "max");
        final Metric.Key metricName3 = new Metric.Key("test-component", "testMetric", "max");

        final MetricFilter filter1 = new IncludeExclude(Collections.singletonList("metric"), Collections.emptyList());
        final MetricFilter filter2 = new IncludeExclude(Collections.singletonList("metric"), Collections.singletonList("component"));
        final MetricFilter filter3 = new IncludeExclude(Collections.singletonList("metric"), Collections.emptyList());

        // when
        final boolean result1 = filter1.accepts(metricName1);
        final boolean result2 = filter2.accepts(metricName2);
        final boolean result3 = filter3.accepts(metricName3);

        // then
        assertTrue(result1);
        assertFalse(result2);
        assertTrue(result3);

    }

    @Test
    public void acceptsIfBothIncludesAndExcludesAreEmpty() {

        // given
        final Metric.Key metricName = new Metric.Key("test-component", "test-metric", "max");
        final MetricFilter filter = new IncludeExclude(Collections.emptyList(), Collections.emptyList());

        // when
        final boolean result = filter.accepts(metricName);

        // then
        assertTrue(result);

    }

    @Test
    public void acceptsIfIncludesContainWildcardAndExcludesAreEmpty() {

        // given
        final Metric.Key metricName = new Metric.Key("test-component", "test-metric", "max");
        final MetricFilter filter = new IncludeExclude(Arrays.asList("*", "other-test-metric"), Collections.emptyList());

        // when
        final boolean result = filter.accepts(metricName);

        // then
        assertTrue(result);

    }

    @Test
    public void acceptsIfIncludesContainMetricNameAndExcludesDoesNot() {

        // given
        final Metric.Key metricName = new Metric.Key("test-component", "test-metric", "max");
        final MetricFilter filter = new IncludeExclude(Collections.singletonList("test-metric"), Collections.singletonList("other-test-metric"));

        // when
        final boolean result = filter.accepts(metricName);

        // then
        assertTrue(result);

    }

    @Test
    public void acceptsIfIncludesContainComponentNameAndExcludesDoesNot() {

        // given
        final Metric.Key metricName = new Metric.Key("test-component", "test-metric", "max");
        final MetricFilter filter = new IncludeExclude(Collections.singletonList("test-component"), Collections.singletonList("other-test-component"));

        // when
        final boolean result = filter.accepts(metricName);

        // then
        assertTrue(result);

    }

    @Test
    public void doesNotAcceptIfIncludesAndExcludesContainDifferentComponentNameAndNoWildcards() {

        // given
        final Metric.Key metricName = new Metric.Key("test-component", "test-metric", "max");
        final MetricFilter filter = new IncludeExclude(Collections.singletonList("other-test-component"), Collections.singletonList("other-test-component"));

        // when
        final boolean result = filter.accepts(metricName);

        // then
        assertFalse(result);

    }

    @Test
    public void doesNotAcceptIfIncludesContainWildcardAndExcludesContainWildcard() {

        // given
        final Metric.Key metricName = new Metric.Key("test-component", "test-metric", "max");
        final MetricFilter filter = new IncludeExclude(Arrays.asList("*", "other-test-metric"), Arrays.asList("*", "other-test-metric"));

        // when
        final boolean result = filter.accepts(metricName);

        // then
        assertFalse(result);

    }

    @Test
    public void doesNotAcceptIfExcludesContainMetricName() {

        // given
        final Metric.Key metricName = new Metric.Key("test-component", "test-metric", "max");
        final MetricFilter filter1 = new IncludeExclude(Collections.singletonList("*"), Collections.singletonList("test-metric"));
        final MetricFilter filter2 = new IncludeExclude(Collections.singletonList("test-metric"), Collections.singletonList("test-metric"));
        final MetricFilter filter3 = new IncludeExclude(Collections.emptyList(), Collections.singletonList("test-metric"));

        // when
        final boolean result1 = filter1.accepts(metricName);
        final boolean result2 = filter2.accepts(metricName);
        final boolean result3 = filter3.accepts(metricName);

        // then
        assertFalse(result1);
        assertFalse(result2);
        assertFalse(result3);

    }

    @Test
    public void doesNotAcceptIfExcludesContainComponentName() {

        // given
        final Metric.Key metricName1 = new Metric.Key("test-component", "test-metric1", "max");
        final Metric.Key metricName2 = new Metric.Key("test-component", "test-metric2", "max");
        final MetricFilter filter1 = new IncludeExclude(Collections.singletonList("*"), Collections.singletonList("test-component"));
        final MetricFilter filter2 = new IncludeExclude(Collections.singletonList("test-metric"), Collections.singletonList("test-component"));
        final MetricFilter filter3 = new IncludeExclude(Collections.singletonList("test-component"), Collections.singletonList("test-component"));
        final MetricFilter filter4 = new IncludeExclude(Collections.emptyList(), Collections.singletonList("test-component"));

        // when
        final boolean result1 = filter1.accepts(metricName1);
        final boolean result2 = filter2.accepts(metricName2);
        final boolean result3 = filter3.accepts(metricName1);
        final boolean result4 = filter4.accepts(metricName1);

        // then
        assertFalse(result1);
        assertFalse(result2);
        assertFalse(result3);
        assertFalse(result4);

    }

    @Test
    public void doesNotAcceptIfExcludesContainWildcard() {

        // given
        final Metric.Key metricName = new Metric.Key("test-component", "test-metric", "max");
        final MetricFilter filter = new IncludeExclude(Collections.emptyList(), Collections.singletonList("*"));

        // when
        final boolean result = filter.accepts(metricName);

        // then
        assertFalse(result);

    }

}
