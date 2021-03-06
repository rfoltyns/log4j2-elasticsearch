package org.appenders.log4j2.elasticsearch;

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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import io.netty.buffer.ByteBuf;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.appenders.log4j2.elasticsearch.thirdparty.ReusableByteBufOutputStream;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ExtendedObjectWriterTest {

    @Test
    public void returnsSameSerProvWhenNoNewSerializersCached() throws IOException {

        // given
        int expectedCalls = new Random().nextInt(100) + 2;
        List<DefaultSerializerProvider> caught = new ArrayList<>();
        ExtendedObjectMapper mapper = new ExtendedObjectMapper(new JsonFactory());
        ExtendedObjectWriter writer = spy((ExtendedObjectWriter)mapper._newWriter(mapper.getSerializationConfig(), mapper.getTypeFactory().constructArrayType(Object.class), null));
        when(writer._serializerProvider()).thenAnswer((Answer<DefaultSerializerProvider>) invocationOnMock -> {
            DefaultSerializerProvider result = (DefaultSerializerProvider) invocationOnMock.callRealMethod();
            caught.add(result);
            return result;
        });

        // when
        for (int i = 0; i < expectedCalls; i++) {
            writer.writeValue((OutputStream)new ReusableByteBufOutputStream(Mockito.mock(ByteBuf.class)), null);
        }

         // then
        assertThat(caught, CoreMatchers.everyItem(CoreMatchers.is(caught.get(0))));
        verify(writer, Mockito.times(expectedCalls))._serializerProvider();

    }

    @Test
    public void returnsNewSerProvNewWhenSerializersCached() throws IOException {

        // given
        int expectedCalls = new Random().nextInt(100) + 2;
        List<DefaultSerializerProvider> caught = new ArrayList<>();
        ExtendedObjectMapper mapper = new ExtendedObjectMapper(new JsonFactory());
        ExtendedObjectWriter writer = spy((ExtendedObjectWriter)mapper._newWriter(mapper.getSerializationConfig(), mapper.getTypeFactory().constructType(Log4jLogEvent.class), null));
        when(writer._serializerProvider()).thenAnswer((Answer<DefaultSerializerProvider>) invocationOnMock -> {
            DefaultSerializerProvider result = (DefaultSerializerProvider) invocationOnMock.callRealMethod();
            caught.add(result);
            return result;
        });

        // when
        for (int i = 0; i < expectedCalls; i++) {
            writer.writeValue((OutputStream)new ReusableByteBufOutputStream(Mockito.mock(ByteBuf.class)), Log4jLogEvent.newBuilder().build());
        }

        // then
        assertEquals(2, new HashSet<>(caught).size());
        verify(writer, Mockito.times(expectedCalls))._serializerProvider();

    }

}
