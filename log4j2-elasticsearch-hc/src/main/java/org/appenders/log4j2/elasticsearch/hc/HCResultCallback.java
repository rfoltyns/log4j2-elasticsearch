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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.concurrent.FutureCallback;

import java.io.IOException;
import java.io.InputStream;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * Generic async callback for Apache HC {@link HttpResponse}s and failures.
 * Adapts {@link HttpResponse} to {@link Response}
 *
 * @param <T> Apache HC response type
 */
public class HCResultCallback<T extends Response> implements FutureCallback<HttpResponse> {

    private final ResponseHandler<T> responseHandler;

    public HCResultCallback(ResponseHandler<T> responseHandler) {
        this.responseHandler = responseHandler;
    }

    @Override
    public void completed(final HttpResponse response) {

        InputStream inputStream = null;
        T result = null;
        try {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                inputStream = entity.getContent();
            }
            result = responseHandler.deserializeResponse(inputStream);

            StatusLine statusLine = response.getStatusLine();
            result.withResponseCode(statusLine.getStatusCode());
            result.withErrorMessage(statusLine.getReasonPhrase());

        } catch (IOException e) {
            failed(e);
        } catch (Throwable t) {
            failed(new Exception("Problem during response processing", t));
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    getLogger().error("Problem closing response input stream", e);
                }
            }
            if (result != null) {
                responseHandler.completed(result);
            }
        }

    }

    @Override
    public void failed(Exception ex) {
        try {
            responseHandler.failed(ex);
        } catch (Exception e) {
            // uncaught exception may cause the client to shutdown
            getLogger().error("Callback failed", e);
        }
    }

    @Override
    public void cancelled() {
        responseHandler.failed(new Exception("Request cancelled"));
    }

}
