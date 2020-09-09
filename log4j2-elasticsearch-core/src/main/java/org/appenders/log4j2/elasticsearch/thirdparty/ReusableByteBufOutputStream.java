/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 * 
 * MODIFICATIONS:
 * rfoltyns: This class is a renamed io.netty.buffer.ByteBufOutputStream with following changes:
 *           writeUTF8(String) not supported
 *           utf8out removed to stop allocating java.io.DataOutputStream (40 bytes)
 *           writtenBytes() removed - wrong results when reused
 */
package org.appenders.log4j2.elasticsearch.thirdparty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.util.CharsetUtil;

import java.io.DataOutput;
import java.io.OutputStream;

/**
 * An {@link OutputStream} which writes data to a {@link ByteBuf}.
 * <p>
 * A write operation against this stream will occur at the {@code writerIndex}
 * of its underlying buffer and the {@code writerIndex} will increase during
 * the write operation.
 * <p>
 * This stream implements {@link DataOutput} for your convenience.
 * The endianness of the stream is not always big endian but depends on
 * the endianness of the underlying buffer.
 *
 * @see ByteBufInputStream
 */
public class ReusableByteBufOutputStream extends OutputStream implements DataOutput {

    private final ByteBuf buffer;

    /**
     * Creates a new stream which writes data to the specified {@code buffer}.
     *
     * @param buffer {@code io.nett.buffer.ByteBuf} to work with
     */
    public ReusableByteBufOutputStream(ByteBuf buffer) {
        if (buffer == null) {
            throw new NullPointerException("buffer");
        }
        this.buffer = buffer;
    }

    @Override
    public void write(byte[] b, int off, int len) {
        if (len == 0) {
            return;
        }

        buffer.writeBytes(b, off, len);
    }

    @Override
    public void write(byte[] b) {
        buffer.writeBytes(b);
    }

    @Override
    public void write(int b) {
        buffer.writeByte(b);
    }

    @Override
    public void writeBoolean(boolean v) {
        buffer.writeBoolean(v);
    }

    @Override
    public void writeByte(int v) {
        buffer.writeByte(v);
    }

    @Override
    public void writeBytes(String s) {
        buffer.writeCharSequence(s, CharsetUtil.US_ASCII);
    }

    @Override
    public void writeChar(int v) {
        buffer.writeChar(v);
    }

    @Override
    public void writeChars(String s) {
        int len = s.length();
        for (int i = 0 ; i < len ; i ++) {
            buffer.writeChar(s.charAt(i));
        }
    }

    @Override
    public void writeDouble(double v) {
        buffer.writeDouble(v);
    }

    @Override
    public void writeFloat(float v) {
        buffer.writeFloat(v);
    }

    @Override
    public void writeInt(int v) {
        buffer.writeInt(v);
    }

    @Override
    public void writeLong(long v) {
        buffer.writeLong(v);
    }

    @Override
    public void writeShort(int v) {
        buffer.writeShort((short) v);
    }

    @Override
    public void writeUTF(String s) {
        throw new UnsupportedOperationException("writeUTF(String) not supported. buffer-based API");
    }

    /**
     * @return underlying buffer
     */
    public ByteBuf buffer() {
        return buffer;
    }

}
