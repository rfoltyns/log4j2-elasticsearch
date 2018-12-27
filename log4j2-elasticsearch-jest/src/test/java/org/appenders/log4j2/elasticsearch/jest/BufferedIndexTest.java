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

import org.appenders.log4j2.elasticsearch.BufferedItemSource;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.mockito.Mockito.mock;

public class BufferedIndexTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void builderFailsWhenSourceIsNull() {

        // given
        BufferedIndex.Builder builder = new BufferedIndex.Builder(null);

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("source cannot be null");

        // when
        builder.build();

    }

    @Test
    public void getDataIsUnsupported() {

        // given
        BufferedIndex bufferedIndex = new BufferedIndex.Builder(mock(BufferedItemSource.class)).build();

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("BufferedIndex cannot return Strings. Use getSource() instead");

        // when
        bufferedIndex.getData(null);

    }

    @Test
    public void createNewElasticSearchResultIsUnsupported() {

        // given
        BufferedIndex bufferedIndex = new BufferedIndex.Builder(mock(BufferedItemSource.class)).build();

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("BufferedIndex cannot handle String result. Use buffer-based API");

        // when
        bufferedIndex.createNewElasticSearchResult(null, 0, null, null);

    }

    @Test
    public void getBulkMethodNameReturnsStaticVariable() {

        // given
        BufferedIndex bufferedIndex = new BufferedIndex.Builder(mock(BufferedItemSource.class)).build();

        // when
        String bullkMethodName = bufferedIndex.getBulkMethodName();

        // then
        Assert.assertEquals(BufferedIndex.BULK_METHOD_NAME, bullkMethodName);

    }


    @Test
    public void getRestMethodNameReturnsStaticVariable() {

        // given
        BufferedIndex bufferedIndex = new BufferedIndex.Builder(mock(BufferedItemSource.class)).build();

        // when
        String restMethodName = bufferedIndex.getRestMethodName();

        // then
        Assert.assertEquals(BufferedIndex.HTTP_METHOD_NAME, restMethodName);

    }

}
