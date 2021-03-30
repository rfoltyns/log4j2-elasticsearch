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
import org.apache.http.Header;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import static org.appenders.log4j2.elasticsearch.GenericItemSourcePoolTest.byteBufAllocator;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ByteBufHttpEntityTest {

    static {
        System.setProperty("io.netty.allocator.maxOrder", "1");
    }

    @Test
    public void writeToOutputStreamIsNotSupported() {

        // given
        ByteBufHttpEntity entity = new ByteBufHttpEntity(createDefaultTestByteBuf(), 0, null);

        // when
        final UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> entity.writeTo(new ByteArrayOutputStream()));

        // then
        assertThat(exception.getMessage(), containsString("writeTo(OutputStream) is not supported. Use getContent() to get InputStream instead"));

    }

    @Test
    public void isRepeatable() {

        // given
        ByteBufHttpEntity entity = new ByteBufHttpEntity(createDefaultTestByteBuf(), 0, null);

        // when
        boolean result = entity.isRepeatable();

        // then
        assertTrue(result);

    }

    @Test
    public void isNotStreaming() {

        // given
        ByteBufHttpEntity entity = new ByteBufHttpEntity(createDefaultTestByteBuf(), 0, null);

        // when
        boolean result = entity.isStreaming();

        // then
        assertFalse(result);

    }

    @Test
    public void setsContentLength() {

        // given
        long expectedContentLength = new Random().nextInt(1000);

        ByteBufHttpEntity entity = new ByteBufHttpEntity(createDefaultTestByteBuf(), expectedContentLength, null);

        // when
        long contentLength = entity.getContentLength();

        // then
        assertEquals(expectedContentLength, contentLength);

    }

    @Test
    public void setsContentTypeIfNotNull() {

        // given
        ContentType expectedContentType = ContentType.APPLICATION_JSON;
        ByteBufHttpEntity entity = new ByteBufHttpEntity(createDefaultTestByteBuf(), 0, expectedContentType);

        // when
        Header contentType = entity.getContentType();

        // then
        assertEquals(expectedContentType.toString(), contentType.getValue());

    }

    @Test
    public void doesntSetContentTypeIfNull() {

        // given
        ByteBufHttpEntity entity = new ByteBufHttpEntity(createDefaultTestByteBuf(), 0, null);

        // when
        Header contentType = entity.getContentType();

        // then
        assertNull(contentType);

    }

    @Test
    public void getContentReturnsInputStreamThatResetsByteBufReaderIndexOnClose() throws IOException {

        // given
        ByteBuf byteBuf = spy(createDefaultTestByteBuf());
        ByteBufHttpEntity entity = new ByteBufHttpEntity(byteBuf, 0, null);

        InputStream inputStream = entity.getContent();

        // when
        inputStream.close();

        // then
        verify(byteBuf).readerIndex(eq(0));

    }

    private CompositeByteBuf createDefaultTestByteBuf() {
        return new CompositeByteBuf(byteBufAllocator, false, 2);
    }

}
