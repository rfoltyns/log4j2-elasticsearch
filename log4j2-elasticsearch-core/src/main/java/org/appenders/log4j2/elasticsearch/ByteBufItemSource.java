package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2018 Rafal Foltynski
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
import org.appenders.log4j2.elasticsearch.thirdparty.ReusableByteBufOutputStream;

import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * {@code io.netty.buffer.ByteBuf} backed {@link ItemSource}.
 * When it's no longer needed, {@link #release()} MUST be called to release underlying resources.
 */
public class ByteBufItemSource implements ItemSource<ByteBuf>, OutputStreamSource {

    private final ByteBuf source;
    private final ReleaseCallback<ByteBuf> releaseCallback;

    public ByteBufItemSource(final ByteBuf source, final ReleaseCallback<ByteBuf> releaseCallback) {
        this.source = source;
        this.releaseCallback = releaseCallback;
    }

    @Override
    public ByteBuf getSource() {
        return source;
    }

    /**
     * Clears the underlying {@code io.netty.buffer.ByteBuf} and invokes the callback
     */
    public void release() {
        source.clear();
        releaseCallback.completed(this);
    }

    @Override
    public OutputStream asOutputStream() {
        return new ReusableByteBufOutputStream(source);
    }

    @Override
    public final OutputStream asOutputStream(final OutputStream outputStream) {

        if (outputStream instanceof ReusableOutputStream) {
            //noinspection unchecked
            ((ReusableOutputStream<ByteBuf>)outputStream).reset(source);
            return outputStream;
        }

        throw new IllegalArgumentException("Not an instance of " + ReusableOutputStream.class.getSimpleName());

    }

    @Override
    public String toString() {
        return source.toString(0, source.writerIndex(), Charset.defaultCharset());
    }

}
