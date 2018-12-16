package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2018 Rafal Foltynski
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

public class NoopIndexNameFormatterTest {

    public static final String TEST_INDEX_NAME = "testIndexName";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void returnsIndexNameUnchanged() {

        // given
        NoopIndexNameFormatter.Builder builder = NoopIndexNameFormatter.newBuilder();
        builder.withIndexName(TEST_INDEX_NAME);
        NoopIndexNameFormatter formatter = builder.build();

        // when
        String formattedIndexName = formatter.format(Mockito.mock(LogEvent.class));

        // then
        assertEquals(TEST_INDEX_NAME, formattedIndexName);
    }

    @Test
    public void builderThrowsWhenNameIsNull() {

        // given
        NoopIndexNameFormatter.Builder builder = NoopIndexNameFormatter.newBuilder();

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("indexName");

        // when
        builder.build();
    }


}
