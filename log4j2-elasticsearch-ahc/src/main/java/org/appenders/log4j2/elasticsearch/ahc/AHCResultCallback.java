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

import org.asynchttpclient.AsyncCompletionHandler;

import java.io.IOException;
import java.io.InputStream;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * Generic async callback for {@link org.asynchttpclient.Response}s and failures.
 * Adapts {@link org.asynchttpclient.Response} to {@link Response}
 *
 * @param <T> response type
 */
public class AHCResultCallback<T extends Response> extends AsyncCompletionHandler<T> {

    private final ResponseHandler<T> responseHandler;

    public AHCResultCallback(final ResponseHandler<T> responseHandler) {
        this.responseHandler = responseHandler;
    }

    @Override
    public void onThrowable(final Throwable t) {
        getLogger().error("{}: Throwable caught: {}", AHCResultCallback.class.getSimpleName(), t.getMessage());
        failed(new RuntimeException(t));
    }

    public void failed(final Exception ex) {
        try {
            responseHandler.failed(ex);
        } catch (Exception e) {
            // uncaught exception may cause the client to shutdown
            getLogger().error("Callback failed", e);
        }
    }

    @Override
    public T onCompleted(final org.asynchttpclient.Response response) {

        InputStream inputStream = null;
        T result = null;
        try {
            if (response.hasResponseBody()) {
                inputStream = response.getResponseBodyAsStream();
            }
            result = responseHandler.deserializeResponse(inputStream);

            result.withResponseCode(response.getStatusCode());
            result.withErrorMessage(response.getStatusText());

        } catch (IOException e) {
            failed(e);
            return null;
        } catch (Throwable t) {
            failed(new Exception("Response processing failed", t));
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    getLogger().error("Exception while closing input stream", e);
                }
            }
            if (result != null) {
                responseHandler.completed(result);
            }
        }

        return result;

    }

}
