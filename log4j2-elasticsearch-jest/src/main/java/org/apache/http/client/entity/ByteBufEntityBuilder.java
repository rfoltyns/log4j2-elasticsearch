package org.apache.http.client.entity;

import io.netty.buffer.ByteBuf;
import org.apache.http.HttpEntity;

/**
 * Custom {@code org.apache.http.client.entity.EntityBuilder} allows to create
 * {@code io.netty.buffer.ByteBuf}-based {@code org.apache.http.HttpEntity}
 */
public class ByteBufEntityBuilder extends EntityBuilder {

    private int contentLength = -1;
    private ByteBuf byteByf;

    public ByteBufEntityBuilder setContentLength(int contentLength) {
        this.contentLength = contentLength;
        return this;
    }

    public ByteBufEntityBuilder setByteBuf(ByteBuf byteBuf) {
        this.byteByf = byteBuf;
        return this;
    }

    @Override
    public HttpEntity build() {
        return new ByteBufHttpEntity(byteByf, contentLength, getContentType());
    }

}
