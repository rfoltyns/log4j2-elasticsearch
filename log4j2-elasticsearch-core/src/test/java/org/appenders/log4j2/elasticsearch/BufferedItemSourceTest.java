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

import io.netty.buffer.CompositeByteBuf;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.appenders.log4j2.elasticsearch.BufferedItemSourcePoolTest.pooledByteBufAllocator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class BufferedItemSourceTest {

    static {
        System.setProperty("io.netty.allocator.maxOrder", "7");
    }

    @Test
    public void doesNotChangeTheSource() {

        // given
        CompositeByteBuf expectedSource = createDefaultTestByteBuf();
        ItemSource itemSource = new BufferedItemSource(expectedSource, mock(ReleaseCallback.class));

        // when
        Object actualSource = itemSource.getSource();

        // then
        assertEquals(expectedSource, actualSource);
        assertTrue(expectedSource == actualSource);

    }

    @Test
    public void releaseDelegatesToGivenCallback() {

        // given
        ReleaseCallback callback = mock(ReleaseCallback.class);
        CompositeByteBuf byteBuf = createDefaultTestByteBuf();

        // then
        ItemSource source = new BufferedItemSource(byteBuf, callback);
        source.release();

        // then
        ArgumentCaptor<ItemSource> captor = ArgumentCaptor.forClass(ItemSource.class);
        verify(callback).completed(captor.capture());

        assertEquals(source, captor.getValue());
        assertTrue(source.getSource() == captor.getValue().getSource());

    }

    @Test
    public void releaseResetsTheSource() {

        // given
        ReleaseCallback callback = mock(ReleaseCallback.class);
        CompositeByteBuf byteBuf = spy(createDefaultTestByteBuf());

        // then
        ItemSource source = new BufferedItemSource(byteBuf, callback);
        source.release();

        // then
        verify(byteBuf).clear();

    }

    private CompositeByteBuf createDefaultTestByteBuf() {
        return new CompositeByteBuf(pooledByteBufAllocator, false, 1);
    }

}
