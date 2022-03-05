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


import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SimpleIndexNameTest {

    public static final String TEST_INDEX_NAME = "testIndexName";

    @Test
    public void returnsIndexNameUnchangedOnAnyObject() {

        // given
        final SimpleIndexName.Builder<Object> builder = new SimpleIndexName.Builder<>();
        builder.withIndexName(TEST_INDEX_NAME);
        final SimpleIndexName<Object> formatter = builder.build();

        // when
        final String formattedIndexName = formatter.format(new Object());

        // then
        assertEquals(TEST_INDEX_NAME, formattedIndexName);

    }

    @Test
    public void returnsIndexNameUnchangedOnAnyMillis() {

        // given
        final SimpleIndexName.Builder<Object> builder = new SimpleIndexName.Builder<>();
        builder.withIndexName(TEST_INDEX_NAME);
        final SimpleIndexName<Object> formatter = builder.build();

        // when
        final String formattedIndexName = formatter.format(new Random().nextLong());

        // then
        assertEquals(TEST_INDEX_NAME, formattedIndexName);

    }

    @Test
    public void builderThrowsWhenNameIsNull() {

        // given
        final SimpleIndexName.Builder<Object> builder = new SimpleIndexName.Builder<>();

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("indexName"));

    }

}
