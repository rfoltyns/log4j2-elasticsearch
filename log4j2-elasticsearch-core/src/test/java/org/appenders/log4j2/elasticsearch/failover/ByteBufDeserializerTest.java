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

import com.fasterxml.jackson.core.JsonParser;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ByteBufDeserializerTest {

    @Test
    public void writesBytesIntoPooledBuffer() throws IOException {

        // given
        ByteBufDeserializer deserializer = spy(new ByteBufDeserializer());
        ByteBufAllocator allocator = mock(ByteBufAllocator.class);
        when(deserializer.allocator()).thenReturn(allocator);

        CompositeByteBuf byteBuf = mock(CompositeByteBuf.class);
        when(allocator.compositeBuffer(eq(2))).thenReturn(byteBuf);

        JsonParser jsonParser = mock(JsonParser.class);
        byte[] bytes = UUID.randomUUID().toString().getBytes();
        when(jsonParser.getBinaryValue()).thenReturn(bytes);

        // when
        deserializer.deserialize(jsonParser, null);

        // then
        verify(byteBuf).writeBytes(eq(bytes));

    }

}
