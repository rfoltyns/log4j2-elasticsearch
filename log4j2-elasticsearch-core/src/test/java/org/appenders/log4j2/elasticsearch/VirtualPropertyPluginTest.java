package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2021 Rafal Foltynski
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

import org.apache.logging.log4j.core.config.ConfigurationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VirtualPropertyPluginTest {

    @Test
    public void builderFailsWhenNameIsNull() {

        // given
        VirtualPropertyPlugin.Builder builder = createDefaultVirtualPropertyBuilder()
                .withName(null);

        // then
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No name provided for " + VirtualProperty.class.getSimpleName()));

    }

    @Test
    public void builderFailsWhenValueIsNull() {

        // given
        VirtualPropertyPlugin.Builder builder = createDefaultVirtualPropertyBuilder()
                .withValue(null);

        // then
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No value provided for " + VirtualProperty.class.getSimpleName()));

    }

    @Test
    public void builderSetsName() {

        // given
        String expectedName = UUID.randomUUID().toString();
        VirtualPropertyPlugin.Builder builder = createDefaultVirtualPropertyBuilder()
                .withName(expectedName);

        // when
        VirtualProperty property = builder.build();

        // then
        assertEquals(expectedName, property.getName());

    }

    @Test
    public void builderSetsValue() {

        // given
        String expectedName = UUID.randomUUID().toString();
        VirtualPropertyPlugin.Builder builder = createDefaultVirtualPropertyBuilder()
                .withName(expectedName);

        // when
        VirtualProperty property = builder.build();

        // then
        assertEquals(expectedName, property.getName());

    }

    @Test
    public void builderSetsDynamic() {

        // given
        VirtualPropertyPlugin.Builder builder = createDefaultVirtualPropertyBuilder()
                .withDynamic(true);

        // when
        VirtualProperty property = builder.build();

        // then
        assertTrue(property.isDynamic());

    }

    @Test
    public void valueCanBeOverridenAfterCreation() {

        // given
        VirtualProperty property = createDefaultVirtualPropertyBuilder().build();
        String expectedValue = UUID.randomUUID().toString();

        assertNotEquals(expectedValue, property.getValue());

        // when
        property.setValue(expectedValue);

        // then
        assertEquals(expectedValue, property.getValue());

    }

    @Test
    public void toStringPrintsFormattedInfo() {

        // given
        String expectedName = UUID.randomUUID().toString();
        String expectedValue = UUID.randomUUID().toString();

        VirtualProperty property = createDefaultVirtualPropertyBuilder()
                .withName(expectedName)
                .withValue(expectedValue)
                .build();

        // when
        String result = property.toString();

        // then
        assertEquals(result, String.format("%s=%s", expectedName, expectedValue));

    }

    public static VirtualPropertyPlugin.Builder createDefaultVirtualPropertyBuilder() {
        return VirtualPropertyPlugin.newBuilder()
                .withName(UUID.randomUUID().toString())
                .withValue(UUID.randomUUID().toString())
                .withDynamic(false);
    }

}
