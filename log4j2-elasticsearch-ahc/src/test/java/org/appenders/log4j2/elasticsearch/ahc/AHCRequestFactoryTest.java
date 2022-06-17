package org.appenders.log4j2.elasticsearch.ahc;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2022 Rafal Foltynski
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
import io.netty.buffer.CompositeByteBuf;
import org.appenders.log4j2.elasticsearch.GenericItemSourcePoolTest;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.asynchttpclient.RequestBuilder;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AHCRequestFactoryTest {

    @Test
    public void doesNotThrowOnUnknownHttpMethodName() throws Exception {

        // given
        final AHCRequestFactory factory = createDefaultTestObject();
        final String expectedUrl = "http://localhost:8080/" + UUID.randomUUID();
        final String httpMethodName = UUID.randomUUID().toString();
        final Request request = createDefaultMockRequest(expectedUrl, httpMethodName);

        // when
        final org.asynchttpclient.Request httpRequest = factory.create(expectedUrl, request).build();

        // then
        assertNotNull(httpRequest);

    }

    @Test
    public void createsPostRequest() throws Exception {

        // given
        final AHCRequestFactory factory = createDefaultTestObject();
        final String expectedUrl = "http://localhost:8080/" + UUID.randomUUID();
        final Request request = createDefaultMockRequest(expectedUrl, "POST");

        // when
        final RequestBuilder result = factory.create(expectedUrl, request);

        // then
        assertEquals("POST", result.build().getMethod());
        assertEquals(result.build().getUri().toString(), new URI(expectedUrl).toString());

    }

    @Test
    public void createsPutRequest() throws Exception {

        // given
        final AHCRequestFactory factory = createDefaultTestObject();
        final String expectedUrl = "http://localhost:8080/" + UUID.randomUUID();
        final Request request = createDefaultMockRequest(expectedUrl, "PUT");

        // when
        final RequestBuilder result = factory.create(expectedUrl, request);

        // then
        assertEquals("PUT", result.build().getMethod());
        assertEquals(result.build().getUri().toString(), new URI(expectedUrl).toString());

    }

    @Test
    public void createsHeadRequest() throws Exception {

        // given
        final AHCRequestFactory factory = createDefaultTestObject();
        final String expectedUrl = "http://localhost:8080/" + UUID.randomUUID();
        final Request request = createDefaultMockRequest(expectedUrl, "HEAD");

        // when
        final RequestBuilder result = factory.create(expectedUrl, request);

        // then
        assertEquals("HEAD", result.build().getMethod());
        assertEquals(result.build().getUri().toString(), new URI(expectedUrl).toString());

    }

    @Test
    public void createsGetRequest() throws Exception {

        // given
        final AHCRequestFactory factory = createDefaultTestObject();
        final String expectedUrl = "http://localhost:8080/" + UUID.randomUUID();
        final Request request = createDefaultMockRequest(expectedUrl, "GET");
        when(request.serialize()).thenReturn(null);

        // when
        final RequestBuilder result = factory.create(expectedUrl, request);

        // then
        assertEquals("GET", result.build().getMethod());
        assertEquals(result.build().getUri().toString(), new URI(expectedUrl).toString());

    }

    @Test
    public void createsEntityUsingGivenSource() throws Exception {

        // given
        final AHCRequestFactory factory = createDefaultTestObject();
        final String expectedUrl = "http://localhost:8080/" + UUID.randomUUID();
        final Request request = createDefaultMockRequest(expectedUrl, "POST");

        final ByteBuf byteBuf = new CompositeByteBuf(GenericItemSourcePoolTest.byteBufAllocator, false, 2);
        final byte[] expectedBytes = UUID.randomUUID().toString().getBytes();
        byteBuf.writeBytes(expectedBytes);

        final ItemSource<ByteBuf> itemSource = mock(ItemSource.class);
        when(itemSource.getSource()).thenReturn(byteBuf);

        when(request.serialize()).thenReturn(itemSource);

        // when
        final RequestBuilder result = factory.create(expectedUrl, request);

        // then
        assertEquals(expectedBytes.length, result.build().getStreamData().available());
        final ByteBuf source = (ByteBuf) request.serialize().getSource();
        source.readBytes(new byte[source.writerIndex()]);

        final InputStream inputStream = result.build().getStreamData();
        assertEquals(0, inputStream.available());

    }

    public static Request createDefaultMockRequest(final String expectedUrl, final String httpMethodName) throws Exception {
        final Request request = mock(Request.class);
        when(request.getURI()).thenReturn(expectedUrl);
        when(request.getHttpMethodName()).thenReturn(httpMethodName);

        final ByteBuf byteBuf = mock(ByteBuf.class);
        when(byteBuf.writerIndex()).thenReturn(1);

        final ItemSource<ByteBuf> itemSource = mock(ItemSource.class);
        when(itemSource.getSource()).thenReturn(byteBuf);

        when(request.serialize()).thenReturn(itemSource);
        return request;
    }

    private AHCRequestFactory createDefaultTestObject() {
        return new AHCRequestFactory();
    }

}
