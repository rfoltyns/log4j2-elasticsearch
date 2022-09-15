package org.appenders.log4j2.elasticsearch;

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

import org.apache.logging.log4j.core.config.ConfigurationException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DataStreamPluginTest {

    public static final String TEST_DATA_STREAM_NAME = "testDataStream";

    private static DataStreamPlugin createTestDataStream(final String name) {
        return DataStreamPlugin.newBuilder()
                .withName(name)
                .build();
    }

    @Test
    public void buildsWithName() {

        // when
        final DataStreamPlugin plugin = createTestDataStream(TEST_DATA_STREAM_NAME);

        // then
        assertNotNull(plugin);
        assertNotNull(plugin.getName());
        assertNotNull(plugin.getSource());
        assertEquals(DataStreamPlugin.TYPE_NAME, plugin.getType());

    }

    @Test
    public void builderThrowsWhenNameIsNotSet() {

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, () -> createTestDataStream(null));

        // then
        assertThat(exception.getMessage(), containsString("No name provided for " + DataStream.class.getSimpleName()));

    }

}
