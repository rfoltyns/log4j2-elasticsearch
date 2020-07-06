package com.fasterxml.jackson.core.json;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2020 Rafal Foltynski
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
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.appenders.log4j2.elasticsearch.OutputStreamDelegate;
import org.appenders.log4j2.elasticsearch.ReusableIOContext;
import org.appenders.log4j2.elasticsearch.ReusableUTF8JsonGenerator;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ReusableUTF8JsonGeneratorTest {

    @Test
    public void canReuseAfterCloseAndReset() throws IOException {

        // given
        OutputStream os1 = mock(OutputStream.class);
        OutputStream os2 = mock(OutputStream.class);

        OutputStreamDelegate delegate = spy(new OutputStreamDelegate(os1));

        ReusableIOContext ctx = createDefaultTestIOContext(delegate);

        ReusableUTF8JsonGenerator gen = createDefaultTestUTF8JsonGenerator(delegate, ctx);

        gen.writeStartObject();
        gen.writeEndObject();

        int expectedLength1 = gen._outputTail;

        // when
        gen.close(); // writes into the buffer

        delegate.setDelegate(os2);
        ctx.setSourceReference(os2);
        gen.reset();

        gen.writeStartObject();
        gen.writeEndObject();

        int expectedLength2 = gen._outputTail;

        gen.close(); // writes into the buffer

        // then
        verify(os1, times(1)).write(eq(gen._outputBuffer), eq(0), eq(expectedLength1));
        verify(os2, times(1)).write(eq(gen._outputBuffer), eq(0), eq(expectedLength2));

        verify(delegate, atLeastOnce()).write(eq(gen._outputBuffer), eq(0), eq(expectedLength1));

    }

    @Test
    public void resetDelegatesToJsonWriteContextAccessor() {

        // given
        OutputStreamDelegate outputStreamDelegate = createDefaultTestOutputStreamDelegate();
        ReusableIOContext ctx = createDefaultTestIOContext(outputStreamDelegate);
        JsonWriteContextAccessor ctxAccess = spy(new JsonWriteContextAccessor());

        ReusableUTF8JsonGenerator jsonGenerator = createDefaultTestUTF8JsonGenerator(outputStreamDelegate, ctx, ctxAccess);

        // when
        jsonGenerator.reset();

        // then
        verify(ctxAccess).reset(any(JsonWriteContext.class));

    }

    @Test
    public void closeClosesJsonContentIfEnabled() throws IOException {

        // given
        OutputStreamDelegate outputStreamDelegate = createDefaultTestOutputStreamDelegate();
        ReusableIOContext ctx = createDefaultTestIOContext(outputStreamDelegate);
        JsonWriteContextAccessor ctxAccess = spy(new JsonWriteContextAccessor());

        ReusableUTF8JsonGenerator jsonGenerator =
                spy(createDefaultTestUTF8JsonGenerator(outputStreamDelegate, ctx, ctxAccess));

        jsonGenerator.configure(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT, true);

        assertTrue(jsonGenerator.getOutputContext().inRoot());
        jsonGenerator.writeStartArray();
        assertTrue(jsonGenerator.getOutputContext().inArray());
        jsonGenerator.writeStartObject();
        assertTrue(jsonGenerator.getOutputContext().inObject());

        // when
        jsonGenerator.close();

        // then
        assertTrue(jsonGenerator.getOutputContext().inRoot());
        assertFalse(jsonGenerator.getOutputContext().inArray());
        assertFalse(jsonGenerator.getOutputContext().inObject());

    }

    @Test
    public void closeDoesNotCloseJsonContentIfNotEnabled() throws IOException {

        // given
        OutputStreamDelegate outputStreamDelegate = createDefaultTestOutputStreamDelegate();
        ReusableIOContext ctx = createDefaultTestIOContext(outputStreamDelegate);
        JsonWriteContextAccessor ctxAccess = spy(new JsonWriteContextAccessor());

        ReusableUTF8JsonGenerator jsonGenerator =
                spy(createDefaultTestUTF8JsonGenerator(outputStreamDelegate, ctx, ctxAccess));

        jsonGenerator.configure(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT, false);

        assertTrue(jsonGenerator.getOutputContext().inRoot());
        jsonGenerator.writeStartArray();
        assertTrue(jsonGenerator.getOutputContext().inArray());
        jsonGenerator.writeStartObject();
        assertTrue(jsonGenerator.getOutputContext().inObject());

        // when
        jsonGenerator.close();

        // then
        assertFalse(jsonGenerator.getOutputContext().inRoot());
        assertFalse(jsonGenerator.getOutputContext().inArray());
        assertTrue(jsonGenerator.getOutputContext().inObject());

    }

    @Test
    public void closeCloseTargetIfEnabled() throws IOException {

        // given
        OutputStreamDelegate outputStreamDelegate = mock(OutputStreamDelegate.class);
        ReusableUTF8JsonGenerator jsonGenerator = createDefaultTestUTF8JsonGenerator(outputStreamDelegate);
        jsonGenerator.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, true);

        // when
        jsonGenerator.close();

        // then
        verify(outputStreamDelegate).close();

    }

    @Test
    public void closeDoesNotCloseTargetIfNotEnabled() throws IOException {

        // given
        OutputStreamDelegate outputStreamDelegate = mock(OutputStreamDelegate.class);
        ReusableUTF8JsonGenerator jsonGenerator = createDefaultTestUTF8JsonGenerator(outputStreamDelegate);
        jsonGenerator.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);

        // when
        jsonGenerator.close();

        // then
        verify(outputStreamDelegate, never()).close();

    }

    @Test
    public void closeFlushesTargetIfEnabled() throws IOException {

        // given
        OutputStreamDelegate outputStreamDelegate = mock(OutputStreamDelegate.class);
        ReusableUTF8JsonGenerator jsonGenerator = createDefaultTestUTF8JsonGenerator(outputStreamDelegate);
        jsonGenerator.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        jsonGenerator.configure(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM, true);

        // when
        jsonGenerator.close();

        // then
        verify(outputStreamDelegate).flush();

    }

    @Test
    public void closeDoesNotFlushTargetIfNotEnabled() throws IOException {

        // given
        OutputStreamDelegate outputStreamDelegate = mock(OutputStreamDelegate.class);
        ReusableUTF8JsonGenerator jsonGenerator = createDefaultTestUTF8JsonGenerator(outputStreamDelegate);
        jsonGenerator.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        jsonGenerator.configure(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM, false);

        // when
        jsonGenerator.close();

        // then
        verify(outputStreamDelegate, never()).flush();

    }

    public static OutputStreamDelegate createDefaultTestOutputStreamDelegate() {
        return new OutputStreamDelegate(mock(OutputStream.class));
    }

    public static ReusableIOContext createDefaultTestIOContext(OutputStream delegate) {
        BufferRecycler recycler = new BufferRecycler();
        return new ReusableIOContext(recycler, delegate, false);
    }

    public static ReusableUTF8JsonGenerator createDefaultTestUTF8JsonGenerator(OutputStreamDelegate outputStreamDelegate) {
        return createDefaultTestUTF8JsonGenerator(
                outputStreamDelegate,
                createDefaultTestIOContext(outputStreamDelegate),
                new JsonWriteContextAccessor());
    }

    public static ReusableUTF8JsonGenerator createDefaultTestUTF8JsonGenerator(
            OutputStreamDelegate outputStreamDelegate,
            ReusableIOContext ctx) {
        return createDefaultTestUTF8JsonGenerator(outputStreamDelegate, ctx, new JsonWriteContextAccessor());
    }

    public static ReusableUTF8JsonGenerator createDefaultTestUTF8JsonGenerator(
            OutputStreamDelegate outputStreamDelegate,
            ReusableIOContext ctx,
            JsonWriteContextAccessor jsonWriteCtxAccess) {
        return createDefaultTestUTF8JsonGenerator(
                outputStreamDelegate,
                ctx,
                JsonGenerator.Feature.collectDefaults(),
                jsonWriteCtxAccess);
    }

    public static ReusableUTF8JsonGenerator createDefaultTestUTF8JsonGenerator(
            OutputStream delegate,
            ReusableIOContext ctx,
            int features,
            JsonWriteContextAccessor jsonWriteCtxAccess) {

        return new ReusableUTF8JsonGenerator(
                ctx,
                features,
                new ObjectMapper(),
                delegate,
                '"',
                jsonWriteCtxAccess);

    }

}
