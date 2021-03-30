package org.appenders.log4j2.elasticsearch.hc.failover;

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
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.failover.FailedItemInfo;
import org.appenders.log4j2.elasticsearch.failover.FailedItemSource;
import org.appenders.log4j2.elasticsearch.hc.IndexRequest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HCFailedItemOpsTest {

    @Test
    public void createsFailedItemSourceFromGivenRequest() {

        // given
        HCFailedItemOps failedItemOps = new HCFailedItemOps();

        ItemSource<ByteBuf> itemSource = mock(ItemSource.class);
        ByteBuf expectedSource = mock(ByteBuf.class);
        when(itemSource.getSource()).thenReturn(expectedSource);

        String expectedIndex = UUID.randomUUID().toString();

        IndexRequest request = createDefaultTestIndexRequest(itemSource, expectedIndex);

        // when
        FailedItemSource result = failedItemOps.createItem(request);

        // then
        assertEquals(expectedSource, result.getSource());
        assertEquals(expectedIndex, result.getInfo().getTargetName());

    }

    @Test
    public void canReuseFailedItemSourceFromGivenRequest() {

        // given
        HCFailedItemOps failedItemOps = new HCFailedItemOps();

        FailedItemSource<ByteBuf> itemSource = mock(FailedItemSource.class);

        IndexRequest request = createDefaultTestIndexRequest(
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
        HCFailedItemOps failedItemOps = new HCFailedItemOps();

        ItemSource<ByteBuf> itemSource = mock(ItemSource.class);
        ByteBuf expectedSource = mock(ByteBuf.class);
        when(itemSource.getSource()).thenReturn(expectedSource);

        String expectedIndex = UUID.randomUUID().toString();

        IndexRequest request = createDefaultTestIndexRequest(itemSource, expectedIndex);

        // when
        FailedItemInfo result = failedItemOps.createInfo(request);

        // then
        assertEquals(expectedIndex, result.getTargetName());

    }

    public IndexRequest createDefaultTestIndexRequest(ItemSource<ByteBuf> itemSource, String expectedIndex) {
        return new IndexRequest.Builder(itemSource)
                .type(UUID.randomUUID().toString())
                .index(expectedIndex)
                .build();
    }

}
