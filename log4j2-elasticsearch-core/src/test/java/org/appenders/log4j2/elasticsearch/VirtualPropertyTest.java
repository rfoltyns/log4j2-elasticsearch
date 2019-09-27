package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2019 Rafal Foltynski
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class VirtualPropertyTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void builderFailsWhenNameIsNull() {

        // given
        VirtualProperty.Builder builder = createDefaultVirtualPropertyBuilder()
                .withName(null);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("No name provided for " + VirtualProperty.PLUGIN_NAME);

        // then
        builder.build();

    }

    @Test
    public void builderFailsWhenValueIsNull() {

        // given
        VirtualProperty.Builder builder = createDefaultVirtualPropertyBuilder()
                .withValue(null);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("No value provided for " + VirtualProperty.PLUGIN_NAME);

        // then
        builder.build();
    }

    @Test
    public void builderSetsName() {

        // given
        String expectedName = UUID.randomUUID().toString();
        VirtualProperty.Builder builder = createDefaultVirtualPropertyBuilder()
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
        VirtualProperty.Builder builder = createDefaultVirtualPropertyBuilder()
                .withName(expectedName);

        // when
        VirtualProperty property = builder.build();

        // then
        assertEquals(expectedName, property.getName());

    }

    @Test
    public void builderSetsDynamic() {

        // given
        VirtualProperty.Builder builder = createDefaultVirtualPropertyBuilder()
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

    public static VirtualProperty.Builder createDefaultVirtualPropertyBuilder() {
        return VirtualProperty.newBuilder()
                .withName(UUID.randomUUID().toString())
                .withValue(UUID.randomUUID().toString())
                .withDynamic(false);
    }

}
