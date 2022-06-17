package org.appenders.log4j2.elasticsearch.ahc;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2022 Rafal Foltynski
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
import io.netty.buffer.ByteBufInputStream;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.asynchttpclient.RequestBuilder;

import java.nio.charset.Charset;

/**
 * Adapts {@link Request} instances to AsyncHttpClient requests
 */
public class AHCRequestFactory implements RequestFactory<RequestBuilder> {

    public RequestBuilder create(final String url, final Request request) throws Exception {

        final RequestBuilder httpUriRequest = new RequestBuilder();

        httpUriRequest.setMethod(request.getHttpMethodName())
                .setUrl(url)
                .setCharset(Charset.defaultCharset())
                .setHeader("Content-Type", "application/json");

        // Still always ByteBuf here. May change in future releases
        //noinspection rawtypes
        final ItemSource itemSource = request.serialize();
        if (itemSource != null) {
            httpUriRequest.setBody(new ByteBufInputStream((ByteBuf) itemSource.getSource()));
        }

        return httpUriRequest;
    }

}
