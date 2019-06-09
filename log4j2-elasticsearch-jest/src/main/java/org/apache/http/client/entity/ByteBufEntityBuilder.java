package org.apache.http.client.entity;

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
