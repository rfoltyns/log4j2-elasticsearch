package org.appenders.log4j2.elasticsearch;

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

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonpCharacterEscapes;
import com.fasterxml.jackson.core.io.IOContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class SingleThreadJsonFactoryTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void createFileBasedGeneratorNotSupported() {

        // given
        SingleThreadJsonFactory factory = new SingleThreadJsonFactory();

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("File not supported. Use OutputStream");

        // when
        factory.createGenerator(mock(File.class), JsonEncoding.UTF8);

    }

    @Test
    public void createGeneratorWithNonUtf8EncodingNotSupported() throws IOException {

        // given
        SingleThreadJsonFactory factory = new SingleThreadJsonFactory();

        expectedException.expect(UnsupportedEncodingException.class);
        expectedException.expectMessage("Encoding not supported: " + JsonEncoding.UTF16_BE.getJavaName());

        // when
        factory.createGenerator(mock(OutputStream.class), JsonEncoding.UTF16_BE);

    }

    @Test
    public void createWriterBasedGeneratorNotSupported() {

        // given
        SingleThreadJsonFactory factory = new SingleThreadJsonFactory();

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("Writer not supported. Use OutputStream");

        // when
        factory.createGenerator(mock(Writer.class));

    }

    @Test
    public void canCreateOutputStreamBasedGenerator() throws IOException {

        // given
        SingleThreadJsonFactory factory = new SingleThreadJsonFactory();
        OutputStream os1 = mock(OutputStream.class);

        // when
        JsonGenerator generator1 = factory.createGenerator(os1, JsonEncoding.UTF8);

        // then
        assertNotNull(generator1);

    }

    @Test
    public void createOutputStreamBasedGeneratorReplacesDelegateTarget() throws IOException {

        // given
        SingleThreadJsonFactory factory = new SingleThreadJsonFactory();
        OutputStream os1 = mock(OutputStream.class);

        factory.createGenerator(os1, JsonEncoding.UTF8);
        assertSame(factory.outputStreamDelegate.getDelegate(), os1);

        // when
        OutputStream os2 = mock(OutputStream.class);
        factory.createGenerator(os2, JsonEncoding.UTF8);

        // then
        assertSame(factory.outputStreamDelegate.getDelegate(), os2);

    }

    @Test
    public void createGeneratorCallsReturnSameInstanceEveryTime() throws IOException {

        // given
        SingleThreadJsonFactory factory = new SingleThreadJsonFactory();

        // when
        JsonGenerator generator1 = factory.createGenerator(mock(OutputStream.class), JsonEncoding.UTF8);
        JsonGenerator generator2 = factory.createGenerator(mock(OutputStream.class), JsonEncoding.UTF8);

        // then
        assertNotNull(generator1);
        assertSame(generator1, generator2);

    }

    @Test
    public void createDataOutputWrapperCallsReplacesDelegateTarget() {

        // given
        SingleThreadJsonFactory factory = new SingleThreadJsonFactory();

        DataOutput dataOutput1 = mock(DataOutput.class);
        factory._createDataOutputWrapper(dataOutput1);
        assertSame(factory.dataOutputDelegate.getDelegate(), dataOutput1);

        // when
        DataOutput dataOutput2 = mock(DataOutput.class);
        factory._createDataOutputWrapper(dataOutput2);

        // then
        assertSame(factory.dataOutputDelegate.getDelegate(), dataOutput2);

    }

    @Test
    public void createDataOutputWrapperCallsReturnSameInstanceEveryTime() {

        // given
        SingleThreadJsonFactory factory = new SingleThreadJsonFactory();

        DataOutput dataOutput1 = mock(DataOutput.class);
        OutputStream outputStream1 = factory._createDataOutputWrapper(dataOutput1);

        // when
        DataOutput dataOutput2 = mock(DataOutput.class);
        OutputStream outputStream2 = factory._createDataOutputWrapper(dataOutput2);

        // then
        assertSame(outputStream1, outputStream2);

    }

    @Test
    public void _createUTF8GeneratorResetsDelegatesAndGenerator() {

        // given
        SingleThreadJsonFactory factory = new SingleThreadJsonFactory();
        OutputStream os1 = mock(OutputStream.class);

        // when
        factory._createUTF8Generator(os1, mock(IOContext.class));

        // then
        assertSame(factory.outputStreamDelegate.getDelegate(), os1);
        assertSame(factory.ioContext.getSourceReference(), os1);

    }

    @Test
    public void canSetCharacterEscapesIfNotNull() throws IOException {

        // given
        SingleThreadJsonFactory factory = new SingleThreadJsonFactory();

        // when
        factory.setCharacterEscapes(JsonpCharacterEscapes.instance());
        JsonGenerator generator = factory.createGenerator(mock(OutputStream.class), JsonEncoding.UTF8);

        // then
        assertSame(JsonpCharacterEscapes.instance(), factory.getCharacterEscapes());
        assertSame(JsonpCharacterEscapes.instance(), generator.getCharacterEscapes());

    }

    @Test
    public void doesNotSetCharacterEscapesIfNull() throws IOException {

        // given
        SingleThreadJsonFactory factory = new SingleThreadJsonFactory();

        factory.setCharacterEscapes(JsonpCharacterEscapes.instance());
        JsonGenerator generator1 = factory.createGenerator(mock(OutputStream.class), JsonEncoding.UTF8);

        // when
        factory.setCharacterEscapes(null);
        JsonGenerator generator2 = factory.createGenerator(mock(OutputStream.class), JsonEncoding.UTF8);

        // then
        assertSame(JsonpCharacterEscapes.instance(), factory.getCharacterEscapes());
        assertSame(JsonpCharacterEscapes.instance(), generator1.getCharacterEscapes());
        assertSame(JsonpCharacterEscapes.instance(), generator2.getCharacterEscapes());

    }

    @Test
    public void canSetRootValueSeparator() throws IOException {

        // given
        SingleThreadJsonFactory factory = new SingleThreadJsonFactory();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // when
        factory.setRootValueSeparator("#");

        JsonGenerator generator = factory.createGenerator(outputStream, JsonEncoding.UTF8);
        generator.writeStartObject();
        generator.writeStringField("field1", "value1");
        generator.writeEndObject();
        generator.close();

        generator.writeStartObject();
        generator.writeStringField("field2", "value2");
        generator.writeEndObject();
        generator.close();

        // then
        assertTrue(outputStream.toString("UTF-8").contains("#"));

    }


    @Test
    public void doesNotSetSeparatorBackToDefaultRootValueSeparator() {

        // given
        SingleThreadJsonFactory factory = new SingleThreadJsonFactory();
        factory.setRootValueSeparator("#");
        assertEquals("#", factory.getRootValueSeparator());

        // when
        factory.setRootValueSeparator(JsonFactory.DEFAULT_ROOT_VALUE_SEPARATOR.getValue());

        // then
        assertEquals("#", factory.getRootValueSeparator());

    }

    @Test
    public void copyDoesNotUseSourceSharedComponents() {

        // given
        SingleThreadJsonFactory factory1 = new SingleThreadJsonFactory();
        factory1.dataOutputDelegate.setDelegate(mock(DataOutput.class));

        // when
        SingleThreadJsonFactory factory2 = factory1.copy();

        // then
        assertNotSame(factory1.outputStreamDelegate, factory2.outputStreamDelegate);
        assertNotSame(factory1.outputStreamDelegate.getDelegate(), factory2.outputStreamDelegate.getDelegate());
        assertNotSame(factory1.dataOutputDelegate, factory2.dataOutputDelegate);
        assertNotSame(factory1.dataOutputDelegate.getDelegate(), factory2.dataOutputDelegate.getDelegate());
        assertNotSame(factory1.ioContext, factory2.ioContext);
        assertNotSame(factory1.ioContext.getSourceReference(), factory2.ioContext.getSourceReference());
        assertNotSame(factory1.jsonGenerator, factory2.jsonGenerator);
        assertNotSame(factory1.writeCtxAccessor, factory2.writeCtxAccessor);

    }

}
