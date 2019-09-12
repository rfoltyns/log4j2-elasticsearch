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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HCRequestFactoryTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void throwsOnUnknownHttpMethodName() throws IOException, URISyntaxException {

        // given
        HCRequestFactory factory = createDefaultTestObject();
        String expectedUrl = UUID.randomUUID().toString();
        String httpMethodName = UUID.randomUUID().toString();
        Request request = createDefaultMockRequest(expectedUrl, httpMethodName);

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage(httpMethodName);

        // when
        HttpUriRequest result = factory.create(expectedUrl, request);

        // then
        assertTrue(result instanceof HttpPost);
        assertEquals(result.getURI(), new URI(expectedUrl));

    }

    @Test
    public void createsPostRequest() throws IOException, URISyntaxException {

        // given
        HCRequestFactory factory = createDefaultTestObject();
        String expectedUrl = UUID.randomUUID().toString();
        Request request = createDefaultMockRequest(expectedUrl, "POST");

        // when
        HttpUriRequest result = factory.create(expectedUrl, request);

        // then
        assertTrue(result instanceof HttpPost);
        assertEquals(result.getURI(), new URI(expectedUrl));

    }

    @Test
    public void createsPutRequest() throws IOException, URISyntaxException {

        // given
        HCRequestFactory factory = createDefaultTestObject();
        String expectedUrl = UUID.randomUUID().toString();
        Request request = createDefaultMockRequest(expectedUrl, "PUT");

        // when
        HttpUriRequest result = factory.create(expectedUrl, request);

        // then
        assertTrue(result instanceof HttpPut);
        assertEquals(result.getURI(), new URI(expectedUrl));

    }

    @Test
    public void createsEntityUsingGivenSource() throws IOException {

        // given
        HCRequestFactory factory = createDefaultTestObject();
        String expectedUrl = UUID.randomUUID().toString();
        Request request = createDefaultMockRequest(expectedUrl, "POST");

        ByteBuf byteBuf = new CompositeByteBuf(UnpooledByteBufAllocator.DEFAULT, false, 2);
        byte[] expectedBytes = UUID.randomUUID().toString().getBytes();
        byteBuf.writeBytes(expectedBytes);

        ItemSource<ByteBuf> itemSource = mock(ItemSource.class);
        when(itemSource.getSource()).thenReturn(byteBuf);

        when(request.serialize()).thenReturn(itemSource);

        // when
        HttpEntityEnclosingRequest result = (HttpEntityEnclosingRequest) factory.create(expectedUrl, request);

        // then
        assertEquals(expectedBytes.length, result.getEntity().getContentLength());
        ByteBuf source = (ByteBuf) request.serialize().getSource();
        source.readBytes(new byte[source.writerIndex()]);

        InputStream inputStream = result.getEntity().getContent();
        assertEquals(0, inputStream.available());

    }

    public static Request createDefaultMockRequest(String expectedUrl, String httpMethodName) throws IOException {
        Request request = mock(Request.class);
        when(request.getURI()).thenReturn(expectedUrl);
        when(request.getHttpMethodName()).thenReturn(httpMethodName);

        ByteBuf byteBuf = mock(ByteBuf.class);
        when(byteBuf.writerIndex()).thenReturn(1);

        ItemSource<ByteBuf> itemSource = mock(ItemSource.class);
        when(itemSource.getSource()).thenReturn(byteBuf);

        when(request.serialize()).thenReturn(itemSource);
        return request;
    }

    private HCRequestFactory createDefaultTestObject() {
        return new HCRequestFactory();
    }

}
