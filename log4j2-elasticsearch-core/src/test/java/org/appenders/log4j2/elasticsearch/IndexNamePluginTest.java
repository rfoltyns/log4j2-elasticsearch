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


import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class IndexNamePluginTest {

    public static final String TEST_INDEX_NAME = "testIndexName";

    @Test
    public void returnsIndexNameUnchangedOnLogEvent() {

        // given
        final IndexNamePlugin.Builder builder = IndexNamePlugin.newBuilder();
        builder.withIndexName(TEST_INDEX_NAME);
        final IndexNamePlugin formatter = builder.build();

        // when
        final String formattedIndexName = formatter.format(Mockito.mock(LogEvent.class));

        // then
        assertEquals(TEST_INDEX_NAME, formattedIndexName);

    }

    @Test
    public void returnsIndexNameUnchangedOnMillis() {

        // given
        final IndexNamePlugin.Builder builder = IndexNamePlugin.newBuilder();
        builder.withIndexName(TEST_INDEX_NAME);
        final IndexNamePlugin formatter = builder.build();

        final LogEvent logEvent = Mockito.mock(LogEvent.class);
        when(logEvent.getTimeMillis()).thenReturn(System.currentTimeMillis());

        // when
        final String formattedIndexName = formatter.format(logEvent.getTimeMillis());

        // then
        assertEquals(TEST_INDEX_NAME, formattedIndexName);

    }

    @Test
    public void builderThrowsWhenNameIsNull() {

        // given
        final IndexNamePlugin.Builder builder = IndexNamePlugin.newBuilder();

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("indexName"));

    }

}
