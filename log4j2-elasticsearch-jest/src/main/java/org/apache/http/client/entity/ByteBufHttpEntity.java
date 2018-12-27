package org.apache.http.client.entity;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.util.Args;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * {@code io.netty.buffer.ByteBuf}-based {@code org.apache.http.HttpEntity}
 */
public class ByteBufHttpEntity extends AbstractHttpEntity {

    private final ByteBuf content;
    private final long length;

    public ByteBufHttpEntity(ByteBuf byteBuf, long length, ContentType contentType) {
        this.content = Args.notNull(byteBuf, "Source input stream");
        this.length = length;
        if (contentType != null) {
            setContentType(contentType.toString());
        }
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }

    @Override
    public boolean isStreaming() {
        return false;
    }

    /**
     * @return the content length or {@code -1} if unknown
     */
    @Override
    public long getContentLength() {
        return this.length;
    }

    /**
     * Returns extended {@code io.netty.buffer.ByteBufInputStream} that resets the {@code io.netty.buffer.ByteBuf} on close
     * 
     * @return {@code io.netty.buffer.ByteBufInputStream}
     */
    @Override
    public InputStream getContent() {
        return new ByteBufInputStream(this.content) {
            @Override
            public void close() throws IOException {
                super.close();
                // let's make it actually repeatable
                content.readerIndex(0);
            }
        };
    }

    /**
     * Unsupported. Content handling available only via {@link #getContent()}
     * 
     * @param outstream target stream
     * @throws UnsupportedOperationException
     */
    @Override
    public void writeTo(final OutputStream outstream) {
        throw new UnsupportedOperationException("writeTo(OutputStream) is not supported. Use getContent() to get InputStream instead");
    }

}