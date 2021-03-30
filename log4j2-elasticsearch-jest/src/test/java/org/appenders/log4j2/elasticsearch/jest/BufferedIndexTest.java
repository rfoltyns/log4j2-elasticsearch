package org.appenders.log4j2.elasticsearch.jest;

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

import org.appenders.log4j2.elasticsearch.ByteBufItemSource;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class BufferedIndexTest {

    @Test
    public void builderFailsWhenSourceIsNull() {

        // given
        BufferedIndex.Builder builder = new BufferedIndex.Builder(null);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("source cannot be null"));

    }

    @Test
    public void getDataIsUnsupported() {

        // given
        BufferedIndex bufferedIndex = new BufferedIndex.Builder(mock(ByteBufItemSource.class)).build();

        // when
        final UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> bufferedIndex.getData(null));

        // then
        assertThat(exception.getMessage(), containsString("BufferedIndex cannot return Strings. Use getSource() instead"));

    }

    @Test
    public void createNewElasticSearchResultIsUnsupported() {

        // given
        BufferedIndex bufferedIndex = new BufferedIndex.Builder(mock(ByteBufItemSource.class)).build();

        // when
        final UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> bufferedIndex.createNewElasticSearchResult(null, 0, null, null));

        // then
        assertThat(exception.getMessage(), containsString("BufferedIndex cannot handle String result. Use buffer-based API"));

    }

    @Test
    public void getReturnsDefaultIfNotSet() {

        // given
        BufferedIndex bufferedIndex = new BufferedIndex.Builder(mock(ByteBufItemSource.class)).build();

        // when
        String type = bufferedIndex.getType();

        // then
        assertNull(type);

    }

    @Test
    public void getBulkMethodNameReturnsNull() {

        // given
        BufferedIndex bufferedIndex = new BufferedIndex.Builder(mock(ByteBufItemSource.class)).build();

        // when
        String bullkMethodName = bufferedIndex.getBulkMethodName();

        // then
        assertNull(bullkMethodName);

    }


    @Test
    public void getRestMethodNameReturnsStaticVariable() {

        // given
        BufferedIndex bufferedIndex = new BufferedIndex.Builder(mock(ByteBufItemSource.class)).build();

        // when
        String restMethodName = bufferedIndex.getRestMethodName();

        // then
        assertEquals(BufferedIndex.HTTP_METHOD_NAME, restMethodName);

    }

}
