package org.appenders.log4j2.elasticsearch.jest.failover;

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

import io.netty.buffer.ByteBuf;
import io.searchbox.action.AbstractDocumentTargetedAction;
import io.searchbox.core.DocumentResult;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.failover.FailedItemInfo;
import org.appenders.log4j2.elasticsearch.failover.FailedItemSource;
import org.appenders.log4j2.elasticsearch.jest.BufferedHttpFailedItemOps;
import org.appenders.log4j2.elasticsearch.jest.BufferedIndex;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BufferedHttpFailedItemOpsTest {

    @Test
    public void createsFailedItemSourceFromGivenRequest() {

        // given
        BufferedHttpFailedItemOps failedItemOps = new BufferedHttpFailedItemOps();

        ItemSource<ByteBuf> itemSource = mock(ItemSource.class);
        ByteBuf expectedSource = mock(ByteBuf.class);
        when(itemSource.getSource()).thenReturn(expectedSource);

        String expectedIndex = UUID.randomUUID().toString();

        AbstractDocumentTargetedAction request = createDefaultTestIndexRequest(itemSource, expectedIndex);

        // when
        FailedItemSource result = failedItemOps.createItem(request);

        // then
        assertEquals(expectedSource, result.getSource());
        assertEquals(expectedIndex, result.getInfo().getTargetName());

    }

    @Test
    public void canReuseFailedItemSourceFromGivenRequest() {

        // given
        BufferedHttpFailedItemOps failedItemOps = new BufferedHttpFailedItemOps();

        FailedItemSource<ByteBuf> itemSource = mock(FailedItemSource.class);

        AbstractDocumentTargetedAction request = createDefaultTestIndexRequest(
                itemSource,
                UUID.randomUUID().toString()
        );

        // when
        FailedItemSource result = failedItemOps.createItem(request);

        // then
        assertSame(itemSource, result);

    }

    @Test
    public void createsFailedItemMetadata() {

        // given
        BufferedHttpFailedItemOps failedItemOps = new BufferedHttpFailedItemOps();

        ItemSource<ByteBuf> itemSource = mock(ItemSource.class);
        ByteBuf expectedSource = mock(ByteBuf.class);
        when(itemSource.getSource()).thenReturn(expectedSource);

        String expectedIndex = UUID.randomUUID().toString();

        AbstractDocumentTargetedAction request = createDefaultTestIndexRequest(itemSource, expectedIndex);

        // when
        FailedItemInfo result = failedItemOps.createInfo(request);

        // then
        assertEquals(expectedIndex, result.getTargetName());

    }

    public AbstractDocumentTargetedAction<DocumentResult> createDefaultTestIndexRequest(ItemSource<ByteBuf> itemSource, String expectedIndex) {
        return new BufferedIndex.Builder(itemSource)
                .type(UUID.randomUUID().toString())
                .index(expectedIndex)
                .build();
    }

}
