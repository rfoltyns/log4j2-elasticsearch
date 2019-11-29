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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import io.netty.buffer.ByteBuf;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ByteBufSerializerTest {

    @Test
    public void handledTypeIsByteBuf() {

        // given
        ByteBufSerializer serializer = new ByteBufSerializer();

        // when
        Class handledType = serializer.handledType();

        // then
        assertEquals(ByteBuf.class, handledType);

    }

    @Test
    public void serializesGivenByteBuf() throws IOException {

        // given
        ByteBufSerializer serializer = new ByteBufSerializer();
        JsonGenerator jsonGenerator = mock(JsonGenerator.class);

        ByteBuf byteBuf = mock(ByteBuf.class);
        when(byteBuf.writerIndex()).thenReturn(10);

        // when
        serializer.serialize(byteBuf, jsonGenerator, mock(SerializerProvider.class));

        // then
        verify(byteBuf).resetReaderIndex();
        verify(jsonGenerator).writeBinary(any(), eq(10));

    }

    @Test
    public void serializeWithTypeDelegatesToSerialize() throws IOException {

        // given
        ByteBufSerializer serializer = spy(new ByteBufSerializer());

        ByteBuf byteBuf = mock(ByteBuf.class);

        JsonGenerator jsonGenerator = mock(JsonGenerator.class);

        SerializerProvider serializerProvider = mock(SerializerProvider.class);

        // when
        serializer.serializeWithType(byteBuf, jsonGenerator, serializerProvider,
                mock(TypeSerializer.class));

        // then
        verify(serializer).serialize(eq(byteBuf), eq(jsonGenerator), eq(serializerProvider));

    }

}
