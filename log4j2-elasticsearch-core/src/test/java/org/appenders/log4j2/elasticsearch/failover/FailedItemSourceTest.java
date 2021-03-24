package org.appenders.log4j2.elasticsearch.failover;

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

import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.StringItemSource;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FailedItemSourceTest {

    @Test
    public void getSourceDelegatesToItemSource() {

        // given
        FailedItemInfo failedItemInfo = mock(FailedItemInfo.class);
        StringItemSource itemSource = mock(StringItemSource.class);
        FailedItemSource<String> failedItemSource = createTestFailedItemSource(itemSource, failedItemInfo);

        String expectedSource = UUID.randomUUID().toString();
        when(itemSource.getSource()).thenReturn(expectedSource);

        // when
        String result = failedItemSource.getSource();

        // then
        assertEquals(expectedSource, result);

    }

    @Test
    public void releaseDelegatesToItemSource() {

        // given
        FailedItemInfo failedItemInfo = mock(FailedItemInfo.class);
        ItemSource<Object> itemSource = mock(ItemSource.class);
        FailedItemSource<Object> failedItemSource = createTestFailedItemSource(itemSource, failedItemInfo);

        // when
        failedItemSource.release();

        // then
        verify(itemSource, times(1)).release();

    }

    @Test
    public void targetNameCanBeRetrieved() {

        // given
        String expectedTargetName = UUID.randomUUID().toString();
        FailedItemInfo failedItemInfo = new FailedItemInfo(expectedTargetName);

        // when
        FailedItemSource failedItemSource = createTestFailedItemSource(mock(ItemSource.class), failedItemInfo);

        // then
        assertEquals(expectedTargetName, failedItemSource.getInfo().getTargetName());
    }

    public static <T> FailedItemSource<T> createTestFailedItemSource(
            ItemSource<T> itemSource,
            FailedItemInfo failedItemInfo) {
        return new FailedItemSource<>(itemSource, failedItemInfo);
    }

}
