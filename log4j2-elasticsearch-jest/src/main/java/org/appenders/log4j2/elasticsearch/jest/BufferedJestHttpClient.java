package org.appenders.log4j2.elasticsearch.jest;

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
import io.searchbox.action.Action;
import io.searchbox.client.JestResult;
import io.searchbox.client.JestResultHandler;
import io.searchbox.client.http.JestHttpClient;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.ByteBufEntityBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;

import java.io.IOException;

/**
 * Extended Jest HTTP client using {@link BufferedBulk} to utilize pooled buffers
 */
public class BufferedJestHttpClient extends JestHttpClient {

    @Override
    public <T extends JestResult> void executeAsync(Action<T> clientRequest, JestResultHandler<? super T> resultHandler) {
        HttpUriRequest request;
        try {
            request = prepareRequest((BufferedBulk)clientRequest);
        } catch (IOException e) {
            resultHandler.failed(e);
            return;
        }

        getAsyncClient().execute(request, responseHandler(clientRequest, resultHandler));

    }

    private <T extends JestResult> BufferedResultCallback<T> responseHandler(Action<T> clientRequest, JestResultHandler<? super T> resultHandler) {
        return new BufferedResultCallback(clientRequest, resultHandler);
    }

    protected HttpUriRequest prepareRequest(final BufferedBulk clientRequest) throws IOException {

        String requestURL = getRequestURL(getNextServer(), clientRequest.getURI());
        HttpUriRequest httpUriRequest = new HttpPost(requestURL);
        ByteBuf byteBuf = clientRequest.serializeRequest();
        ByteBufEntityBuilder entityBuilder = (ByteBufEntityBuilder) new ByteBufEntityBuilder()
                .setByteBuf(byteBuf)
                .setContentLength(byteBuf.writerIndex())
                .setContentType(requestContentType);
        ((HttpEntityEnclosingRequest) httpUriRequest).setEntity(entityBuilder.build());

        return httpUriRequest;
    }

    public class BufferedResultCallback<T extends JestResult> implements FutureCallback<HttpResponse> {

        private final Action<T> clientRequest;
        private final JestResultHandler<T> resultHandler;

        public BufferedResultCallback(Action<T> clientRequest, JestResultHandler<T> request) {
            this.clientRequest = clientRequest;
            this.resultHandler = request;
        }

        @Override
        public void completed(final HttpResponse response) {

            BufferedJestResult jestResult = new BufferedJestResult();

            try {

                StatusLine statusLine = response.getStatusLine();
                BufferedBulkResult bulkResult = ((BufferedBulk)clientRequest).deserializeResponse(response.getEntity().getContent());
                jestResult.setSucceeded(bulkResult.isSucceeded());
                jestResult.setResponseCode(statusLine.getStatusCode());
                jestResult.setErrorMessage(bulkResult.getErrorMessage(statusLine.getReasonPhrase()));
                jestResult.setItems(bulkResult.getItems());

            } catch (IOException e) {
                failed(e);
                return;
            } catch (Throwable t) {
                failed(new Exception("Problem during request processing", t));
                return;
            }

            resultHandler.completed((T) jestResult);

        }

        @Override
        public void failed(Exception ex) {
            resultHandler.failed(ex);
        }

        @Override
        public void cancelled() {
            resultHandler.failed(new Exception("Request cancelled"));
        }

    }

}

