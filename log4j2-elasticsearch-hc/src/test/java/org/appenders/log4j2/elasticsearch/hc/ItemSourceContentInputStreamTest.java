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
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ItemSourceContentInputStreamTest {

    @Test
    public void closeReleasesBufferOnlyOnce() throws IOException {

        // given
        ItemSource itemSource = mock(ItemSource.class);
        when(itemSource.getSource()).thenReturn(mock(SimpleInputBuffer.class));

        InputStream inputStream = new ItemSourceContentInputStream(itemSource);

        // when
        inputStream.close();
        inputStream.close();

        // then
        verify(itemSource, times(1)).release();

    }

}
