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
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;

import java.io.IOException;

/**
 * Adapts {@link Request} instances to Apache HC requests
 */
public class HCRequestFactory implements RequestFactory<HttpUriRequest> {

    protected ContentType requestContentType = ContentType.APPLICATION_JSON.withCharset("utf-8");

    public HttpUriRequest create(String url, Request request) throws IOException {

        HttpUriRequest httpUriRequest;

        if (request.getHttpMethodName().equalsIgnoreCase("POST")) {
            httpUriRequest = new HttpPost(url);
        } else if (request.getHttpMethodName().equalsIgnoreCase("GET")) {
            httpUriRequest = new HttpGet(url);
        } else if (request.getHttpMethodName().equalsIgnoreCase("PUT")) {
            httpUriRequest = new HttpPut(url);
        } else if (request.getHttpMethodName().equalsIgnoreCase("HEAD")) {
            httpUriRequest = new HttpHead(url);
        } else {
            throw new UnsupportedOperationException(request.getHttpMethodName());
        }

        if (httpUriRequest instanceof HttpEntityEnclosingRequest) {
            ((HttpEntityEnclosingRequest)httpUriRequest).setEntity(createHttpEntity(request));
        }

        return httpUriRequest;
    }

    protected HttpEntity createHttpEntity(Request request) throws IOException {

        ByteBuf byteBuf = (ByteBuf) request.serialize().getSource();

        return new ByteBufEntityBuilder()
                .setByteBuf(byteBuf)
                .setContentLength(byteBuf.writerIndex())
                .setContentType(requestContentType)
                .build();

    }

}
