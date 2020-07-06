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
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.JsonWriteContextAccessor;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

/**
 * Instances of this class are NOT thread safe!
 * <p></p>
 * Instances of {@code com.fasterxml.jackson.core.JsonGenerator} based
 * writers created by this class are NOT tread safe!
 * <p></p>
 * Allows to reuse <i>ONE(!)</i> {@code com.fasterxml.jackson.core.JsonGenerator}
 * for every {@link #_createUTF8Generator(OutputStream, IOContext)} call
 * by replacing several relevant components with one-per-factory counterparts.
 * <p></p>
 * Unless accessed exclusively by a single thread at once,
 * concurrent calls to writers based on this factory
 * will eventually lead to serialization errors as
 * instances of {@code com.fasterxml.jackson.core.io.IOContext} and
 * {@code com.fasterxml.jackson.core.json.JsonWriteContext} are shared.
 */
public class SingleThreadJsonFactory extends JsonFactory {

    protected final DataOutputAsStreamDelegate dataOutputDelegate = new DataOutputAsStreamDelegate(null);
    protected final OutputStreamDelegate outputStreamDelegate = new OutputStreamDelegate(dataOutputDelegate);
    protected final JsonWriteContextAccessor writeCtxAccessor = new JsonWriteContextAccessor();
    protected final ReusableIOContext ioContext = new ReusableIOContext(_getBufferRecycler(), outputStreamDelegate, false);
    protected final ReusableUTF8JsonGenerator jsonGenerator = singleThreadUTF8JsonGenerator(ioContext);

    /**
     * Creates default, ready-to-use instance
     */
    public SingleThreadJsonFactory() {
        super();
    }

    /**
     * Used by {@link #copy()}.
     * <p></p>
     * Copies only inherited fields.
     *
     * @param factory source factory
     * @param codec codec to use with new factory
     */
    public SingleThreadJsonFactory(SingleThreadJsonFactory factory, ObjectCodec codec) {
        super(factory, codec);
    }

    /**
     * Not supported
     *
     * @param w irrelevant
     * @return throws
     * @throws UnsupportedOperationException {@code java.io.UnsupportedEncodingException} is always thrown here
     */
    @Override
    public JsonGenerator createGenerator(Writer w) {
        throw new UnsupportedOperationException("Writer not supported. Use OutputStream");
    }

    /**
     * Not supported
     *
     * @param f irrelevant
     * @param enc irrelevant
     * @return throws
     * @throws UnsupportedOperationException {@code java.io.UnsupportedEncodingException} is always thrown here
     */
    @Override
    public JsonGenerator createGenerator(File f, JsonEncoding enc) {
        throw new UnsupportedOperationException("File not supported. Use OutputStream");
    }

    /**
     * Returns current {@code com.fasterxml.jackson.core.JsonGenerator} instance with new target.
     * Supports UTF-8 only.
     *
     * @param out new {@code java.io.OutputStream} to write to
     * @param enc MUST be UTF-8
     * @return write-ready {@code com.fasterxml.jackson.core.JsonGenerator}
     * @throws IOException if {@code enc} is not UTF-8
     */
    @Override
    public JsonGenerator createGenerator(OutputStream out, JsonEncoding enc) throws IOException {

        if (enc != JsonEncoding.UTF8) {
            throw new UnsupportedEncodingException("Encoding not supported: " + enc.getJavaName());
        }

        return _createUTF8Generator(_decorate(out, ioContext), ioContext);

    }

    /**
     * Replaces target of {@link #dataOutputDelegate} with new {@link java.io.DataOutput}
     *
     * @param out new {@code java.io.DataOutput}
     * @return {@link #dataOutputDelegate} with new target
     */
    @Override
    protected OutputStream _createDataOutputWrapper(DataOutput out) {
        dataOutputDelegate.setDelegate(out);
        return dataOutputDelegate;
    }

    /**
     * Returns current {@code com.fasterxml.jackson.core.JsonGenerator} instance with new target
     *
     * @param out new {@code java.io.OutputStream} to write to
     * @param ctxt omitted, reusing {@link #ioContext}
     * @return write-ready {@code com.fasterxml.jackson.core.JsonGenerator}
     *
     */
    @Override
    protected JsonGenerator _createUTF8Generator(OutputStream out, IOContext ctxt) {
        outputStreamDelegate.setDelegate(out);
        ioContext.setSourceReference(out);
        return jsonGenerator.reset();
    }

    @Override
    public final JsonFactory setCharacterEscapes(CharacterEscapes esc) {

        if (esc != null) {
            super.setCharacterEscapes(esc);
            jsonGenerator.setCharacterEscapes(getCharacterEscapes());
        }

        return this;
    }

    /**
     * Since it may allocate {@code com.fasterxml.jackson.core.SerializableString}, configure only once.
     *
     * @param sep new separator
     * @return this
     */
    @Override
    public JsonFactory setRootValueSeparator(String sep) {

        if (!DefaultPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR.getValue().equals(sep)) {
            super.setRootValueSeparator(sep);
            jsonGenerator.setRootValueSeparator(_rootValueSeparator);
        }

        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SingleThreadJsonFactory copy()
    {
        _checkInvalidCopy(SingleThreadJsonFactory.class);
        return new SingleThreadJsonFactory(this, null);
    }

    private ReusableUTF8JsonGenerator singleThreadUTF8JsonGenerator(IOContext ctxt) {
        return new ReusableUTF8JsonGenerator(
                ctxt,
                _generatorFeatures,
                _objectCodec, // FIXME: add ObjectCodecDelegate
                outputStreamDelegate,
                _quoteChar,
                writeCtxAccessor
        );
    }

}
