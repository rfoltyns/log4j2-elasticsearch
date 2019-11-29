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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HeapBytesStore;
import net.openhft.chronicle.bytes.VanillaBytes;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import org.appenders.log4j2.elasticsearch.ByteBufItemSource;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.thirdparty.ReusableByteBufOutputStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class FailedItemMarshallerTest {

    @Test
    public void defaultConstructorCreatesObjectMapper() {

        // given
        ObjectMapper configuredObjectMapper = new ObjectMapper();

        AtomicInteger callCount = new AtomicInteger();

        // when
        new FailedItemMarshaller() {
            @Override
            ObjectMapper createConfiguredObjectMapper() {
                callCount.incrementAndGet();
                return configuredObjectMapper;
            }
        };

        // then
        assertEquals(1, callCount.get());

    }

    @Test
    public void readResolveCreatesMapperOnlyOnce() {

        // given
        ObjectMapper configuredObjectMapper = new ObjectMapper();

        AtomicInteger callCount = new AtomicInteger();
        FailedItemMarshaller failedItemMarshaller = new FailedItemMarshaller() {
            @Override
            ObjectMapper createConfiguredObjectMapper() {
                callCount.incrementAndGet();
                return configuredObjectMapper;
            }
        };

        // when
        failedItemMarshaller.readResolve();
        failedItemMarshaller.readResolve();

        // then
        assertEquals(1, callCount.get());

    }

    @Test
    public void returnedObjectMapperIsACopyOfInternalMapper() {

        // given
        ObjectMapper configuredObjectMapper = new ObjectMapper();
        FailedItemMarshaller failedItemMarshaller = new FailedItemMarshaller() {
            @Override
            ObjectMapper createConfiguredObjectMapper() {
                return configuredObjectMapper;
            }
        };

        // when
        ObjectMapper result = failedItemMarshaller.objectMapper();

        // then
        assertNotSame(configuredObjectMapper, result);

    }

    @Test
    public void readDoesNotRethrow() {

        // given
        FailedItemMarshaller failedItemMarshaller = new FailedItemMarshaller();

        Bytes bytes = mock(Bytes.class);
        RuntimeException expectedException = spy(new RuntimeException("test exception"));
        when(bytes.inputStream()).thenThrow(expectedException);

        // when
        failedItemMarshaller.read(bytes, null);

        // then
        verify(expectedException, times(1)).getMessage();

    }

    @Test
    public void writeDoesNotRethrow() {

        // given
        FailedItemMarshaller failedItemMarshaller = new FailedItemMarshaller();

        Bytes bytes = mock(Bytes.class);
        RuntimeException expectedException = spy(new RuntimeException("test exception"));
        when(bytes.outputStream()).thenThrow(expectedException);

        // when
        failedItemMarshaller.write(bytes, mock(ItemSource.class));

        // then
        verify(expectedException, times(1)).getMessage();

    }

    @Test
    public void readMarshallableHasNoSideEffects() {

        // given
        FailedItemMarshaller failedItemMarshaller = new FailedItemMarshaller();

        WireIn wireIn = mock(WireIn.class);

        // when
        failedItemMarshaller.readMarshallable(wireIn);

        // then
        verifyZeroInteractions(wireIn);

    }

    @Test
    public void writeMarshallableHasNoSideEffects() {

        // given
        FailedItemMarshaller failedItemMarshaller = new FailedItemMarshaller();

        WireOut wireOut = mock(WireOut.class);

        // when
        failedItemMarshaller.writeMarshallable(wireOut);

        // then
        verifyZeroInteractions(wireOut);

    }

    @Test
    public void jacksonInjectedReleaseCallbackDoesNotThrow() throws IOException {

        // given
        FailedItemMarshaller failedItemMarshaller = new FailedItemMarshaller();

        CompositeByteBuf byteBuf = new CompositeByteBuf(PooledByteBufAllocator.DEFAULT, false, 2).capacity(1024);
        ByteBufItemSource itemSource = new ByteBufItemSource(byteBuf, source -> {});
        FailedItemSource<ByteBuf> failedItemSource =
                new FailedItemSource<>(itemSource, new FailedItemInfo(UUID.randomUUID().toString()));

        ReusableByteBufOutputStream outputStream =
                new ReusableByteBufOutputStream(byteBuf);
        failedItemMarshaller.objectMapper().writeValue((OutputStream) outputStream, failedItemSource);

        Bytes<Object> in = mock(Bytes.class);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(byteBuf.array());
        when(in.inputStream()).thenReturn(inputStream);

        FailedItemSource deserialized = (FailedItemSource) failedItemMarshaller.read(in, null);

        // when
        deserialized.release();

    }
}
