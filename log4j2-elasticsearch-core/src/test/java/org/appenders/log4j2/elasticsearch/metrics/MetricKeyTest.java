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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MetricKeyTest {

    @Test
    public void comparesComponentPartAndMetricPartOnly() {

        for (int i = 0; i < 1000; i++) {

            // given
            final String componentPart1 = "component1";
            final String metricNamePart1 = "metric1";
            final String componentPart2 = "component2";
            final String metricNamePart2 = "metric2";

            final Metric.Key key1 = new Metric.Key(componentPart1, metricNamePart1, "type2");
            final Metric.Key key2 = new Metric.Key(componentPart1, metricNamePart1, "type1");
            final Metric.Key key3 = new Metric.Key(componentPart1, metricNamePart1, "type2");
            final Metric.Key key4 = new Metric.Key(componentPart2, metricNamePart1, "type1");
            final Metric.Key key5 = new Metric.Key(componentPart1, metricNamePart2, "type1");

            // when
            final int result1 = key1.compareTo(key2);
            final int result2 = key1.compareTo(key3);
            final int result3 = key2.compareTo(key3);
            final int result4 = key1.compareTo(key4);
            final int result5 = key1.compareTo(key5);

            // then
            assertEquals(0, result1);
            assertEquals(0, result2);
            assertEquals(0, result3);
            assertEquals(-1, result4);
            assertEquals(-1, result5);

        }

    }

    @Test
    public void equalsAndHashCodeContract() {

        for (int i = 0; i < 1000; i++) {

            // given
            final String componentPart1 = UUID.randomUUID().toString();
            final String metricNamePart1 = UUID.randomUUID().toString();
            final String componentPart2 = UUID.randomUUID().toString();
            final String metricNamePart2 = UUID.randomUUID().toString();
            final Metric.Key key1 = new Metric.Key(componentPart1, metricNamePart1, "type1");
            final Metric.Key key2 = new Metric.Key(componentPart2, metricNamePart1, "type1");
            final Metric.Key key3 = new Metric.Key(componentPart1, metricNamePart2, "type1");
            final Metric.Key key4 = new Metric.Key(componentPart1, metricNamePart1, "type2");

            // when
            final boolean result1 = key1.equals(key2);
            @SuppressWarnings("EqualsWithItself")
            final boolean result2 = key1.equals(key1);
            final boolean result3 = key1.equals(new Object());
            @SuppressWarnings("ConstantConditions")
            final boolean result4 = key1.equals(null);
            final boolean result5 = key1.equals(key3);
            final boolean result6 = key1.equals(key4);
            final int hashCode1 = key1.hashCode();
            final int hashCode2 = key2.hashCode();
            final int hashCode3 = key3.hashCode();
            final int hashCode4 = key4.hashCode();

            // then
            assertFalse(result1);
            //noinspection ConstantConditions
            assertTrue(result2);
            assertFalse(result3);
            //noinspection ConstantConditions
            assertFalse(result4);
            assertFalse(result5);
            assertTrue(result6);
            assertNotEquals(hashCode1, hashCode2);
            assertNotEquals(hashCode1, hashCode3);
            assertEquals(hashCode1, hashCode4);

        }

    }

    @Test
    public void toStringPrintsFullKey() {

        // given
        final String componentPart = UUID.randomUUID().toString();
        final String metricNamePart = UUID.randomUUID().toString();
        final String metricTypePart = UUID.randomUUID().toString();

        final String expectedKey = componentPart + "." + metricNamePart + "." + metricTypePart;
        final Metric.Key key = new Metric.Key(componentPart, metricNamePart, metricTypePart);

        // when
        final CharSequence result = key.toString();

        // then
        assertEquals(expectedKey, result);

    }

    @Test
    public void retrievesEachPart() {

        // given
        final String componentPart = UUID.randomUUID().toString();
        final String metricNamePart = UUID.randomUUID().toString();
        final String metricTypePart = UUID.randomUUID().toString();

        // when
        final Metric.Key key = new Metric.Key(componentPart, metricNamePart, metricTypePart);

        // then
        assertEquals(componentPart, key.getComponentNamePart());
        assertEquals(metricNamePart, key.getMetricNamePart());
        assertEquals(metricTypePart, key.getMetricTypePart());

    }

}
