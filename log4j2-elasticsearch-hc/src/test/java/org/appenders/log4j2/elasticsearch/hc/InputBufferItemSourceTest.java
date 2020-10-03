package org.appenders.log4j2.elasticsearch.hc;

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

import org.apache.http.nio.util.SimpleInputBuffer;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.ReleaseCallback;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class InputBufferItemSourceTest {

    @Test
    public void getSourceReturnsSameItemSourceInstance() {

        // given
        SimpleInputBuffer expected = mock(SimpleInputBuffer.class);

        // when
        ItemSource<SimpleInputBuffer> itemSource = new InputBufferItemSource(expected, null);

        // then
        Assert.assertTrue(expected == itemSource.getSource());

    }

    @Test
    public void releaseDelegatesToReleaseCallback() {

        // given
        SimpleInputBuffer buffer = mock(SimpleInputBuffer.class);
        ReleaseCallback releaseCallback = mock(ReleaseCallback.class);
        ItemSource<SimpleInputBuffer> itemSource = new InputBufferItemSource(buffer, releaseCallback);

        // when
        itemSource.release();

        // then
        verify(releaseCallback, times(1)).completed(eq(itemSource));

    }

}
