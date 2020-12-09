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

import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.UTF8JsonGenerator;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Instances of this class can be used again after {@link #close()}
 */
public class ReusableUTF8JsonGenerator extends UTF8JsonGenerator {

    private final JsonWriteContextAccessor jsonWriteCtxAccess;

    public ReusableUTF8JsonGenerator(
            IOContext ctxt,
            int generatorFeatures,
            ObjectCodec objectCodec,
            OutputStream outputStream,
            char _quoteChar,
            JsonWriteContextAccessor jsonWriteCtxAccess) {
        super(ctxt, generatorFeatures, objectCodec, outputStream, _quoteChar);
        this.jsonWriteCtxAccess = jsonWriteCtxAccess;
    }

    /**
     * Resets for new write.
     * <br>
     * Does NOT release buffers.
     * <br>
     * <p>{@code com.fasterxml.jackson.core.JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT} supported</p>
     * <p>{@code com.fasterxml.jackson.core.JsonGenerator.Feature.AUTO_CLOSE_TARGET} supported</p>
     * <p>{@code com.fasterxml.jackson.core.JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM} supported</p>
     */
    @Override
    public void close() throws IOException {

        // rfoltyns: Not closing actually. _closed still false here

        // rfoltyns: _releaseBuffers() never called -> _outputBuffer never null
        if (isEnabled(Feature.AUTO_CLOSE_JSON_CONTENT)) {
            while (true) {
                JsonStreamContext ctxt = getOutputContext();
                if (ctxt.inArray()) {
                    writeEndArray();
                } else if (ctxt.inObject()) {
                    writeEndObject();
                } else {
                    break;
                }
            }
        }
        _flushBuffer();
        _outputTail = 0;

        // rfoltyns: _outputStream never null here
        if (isEnabled(Feature.AUTO_CLOSE_TARGET)) {
            _outputStream.close();
        } else if (isEnabled(Feature.FLUSH_PASSED_TO_STREAM)) {
            _outputStream.flush();
        }

        // rfoltyns: Don't release buffers, reusing whole instance here

    }

    /**
     * Allows to reset underlying {@code _writeContext} before serializing new object.
     *
     * @return this
     */
    public ReusableUTF8JsonGenerator reset() {
        _writeContext = jsonWriteCtxAccess.reset(_writeContext);
        return this;
    }

}
