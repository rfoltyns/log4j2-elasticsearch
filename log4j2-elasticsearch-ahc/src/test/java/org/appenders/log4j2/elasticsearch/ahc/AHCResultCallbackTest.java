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

import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import static org.appenders.core.logging.InternalLoggingTest.mockTestLogger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AHCResultCallbackTest {

    @Test
    public void doesNotFailWhenEntityIsNull() throws IOException {

        // given
        final BasicResponse basicResponse = new BasicResponse();

        //noinspection unchecked
        final ResponseHandler<Response> responseHandler = mock(ResponseHandler.class);
        when(responseHandler.deserializeResponse(isNull())).thenReturn(basicResponse);

        final AHCResultCallback<Response> callback = new AHCResultCallback<>(responseHandler);

        final org.asynchttpclient.Response httpResponse = mock(org.asynchttpclient.Response.class);
        when(httpResponse.getResponseBodyAsStream()).thenReturn(null);

        final int expectedStatusCode = 1;
        final String expectedReason = UUID.randomUUID().toString();
        when(httpResponse.getStatusCode()).thenReturn(expectedStatusCode);
        when(httpResponse.getStatusText()).thenReturn(expectedReason);

        // when
        callback.onCompleted(httpResponse);

        // then
        verify(responseHandler).deserializeResponse(isNull());
        verify(responseHandler, never()).failed(any());

    }

    @Test
    public void httpResponseStatusCodeOverridesBodyStatusCode() throws IOException {

        // given
        final int notExpectedCode = 2;
        final BatchResult batchResult = new BatchResult(0, false, null, notExpectedCode, new ArrayList<>());

        //noinspection unchecked
        final ResponseHandler<Response> responseHandler = mock(ResponseHandler.class);
        when(responseHandler.deserializeResponse(isNull())).thenReturn(batchResult);

        final AHCResultCallback<Response> callback = new AHCResultCallback<>(responseHandler);

        final org.asynchttpclient.Response httpResponse = mock(org.asynchttpclient.Response.class);
        when(httpResponse.getResponseBodyAsStream()).thenReturn(null);

        final int expectedStatusCode = 1;
        final String expectedReason = UUID.randomUUID().toString();
        when(httpResponse.getStatusCode()).thenReturn(expectedStatusCode);
        when(httpResponse.getStatusText()).thenReturn(expectedReason);

        // when
        final Response response = callback.onCompleted(httpResponse);

        // then
        verify(responseHandler).deserializeResponse(isNull());
        verify(responseHandler, never()).failed(any());
        assertEquals(expectedStatusCode, response.getResponseCode());

    }

    @Test
    public void logsOnResponseHandlerFailureHandlingExceptions() {

        // given
        final Logger logger = mockTestLogger();

        //noinspection unchecked
        final ResponseHandler<Response> responseHandler = mock(ResponseHandler.class);
        final RuntimeException exception = new RuntimeException("test handler exception");
        doThrow(exception).when(responseHandler).failed(any());

        final AHCResultCallback<Response> callback = new AHCResultCallback<>(responseHandler);

        // when
        callback.failed(new Exception("any"));

        // then
        verify(logger).error(eq("Callback failed"), same(exception));

        InternalLogging.setLogger(null);

    }

    @Test
    public void logsOnCompletionHandlerThrowable() {

        // given
        final Logger logger = mockTestLogger();

        final AHCResultCallback<Response> callback = new AHCResultCallback<>(mock(ResponseHandler.class));
        final String expectedMessage = UUID.randomUUID().toString();
        final Exception exception = new Exception(expectedMessage);

        // when
        callback.onThrowable(exception);

        // then
        verify(logger).error(eq("{}: Throwable caught: {}"), eq(AHCResultCallback.class.getSimpleName()), same(exception.getMessage()));

        InternalLogging.setLogger(null);

    }

    @Test
    public void logsOnInputStreamCloseExceptions() throws IOException {

        // given
        final Logger logger = mockTestLogger();

        //noinspection unchecked
        final ResponseHandler<Response> responseHandler = mock(ResponseHandler.class);
        when(responseHandler.deserializeResponse(any())).thenReturn(new BatchResult(0, true, null, 500, Collections.emptyList()));

        final AHCResultCallback<Response> callback = new AHCResultCallback<>(responseHandler);

        final IOException exception = new IOException("input stream close exception test");
        final ByteArrayInputStream byteArrayInputStream = spy(new ByteArrayInputStream(new byte[0]));
        doThrow(exception).when(byteArrayInputStream).close();

        final org.asynchttpclient.Response httpResponse = mockHttpResponse();
        when(httpResponse.hasResponseBody()).thenReturn(true);
        when(httpResponse.getResponseBodyAsStream()).thenReturn(byteArrayInputStream);

        // when
        callback.onCompleted(httpResponse);

        // then
        verify(responseHandler).deserializeResponse(isNotNull());
        verify(logger).error(eq("Exception while closing input stream"), same(exception));

        InternalLogging.setLogger(null);

    }

    @Test
    public void delegatesToResponseFailureHandlerOnThrowable() throws IOException {

        // given
        //noinspection unchecked
        final ResponseHandler<Response> responseHandler = mock(ResponseHandler.class);
        final Exception exception = new RuntimeException("test handler exception");
        when(responseHandler.deserializeResponse(any())).thenThrow(exception);

        final AHCResultCallback<Response> callback = new AHCResultCallback<>(responseHandler);

        final org.asynchttpclient.Response httpResponse = mockHttpResponse();
        when(httpResponse.hasResponseBody()).thenReturn(true);

        // when
        callback.onCompleted(httpResponse);

        // then
        verify(responseHandler).deserializeResponse(isNotNull());

        final ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(responseHandler).failed(captor.capture());

        assertSame(exception, captor.getValue().getCause());

    }

    @Test
    public void delegatesToResponseFailureHandlerOnIOException() throws IOException {

        // given
        //noinspection unchecked
        final ResponseHandler<Response> responseHandler = mock(ResponseHandler.class);
        final IOException exception = new IOException("test handler exception");
        when(responseHandler.deserializeResponse(any())).thenThrow(exception);

        final AHCResultCallback<Response> callback = new AHCResultCallback<>(responseHandler);

        final org.asynchttpclient.Response httpResponse = mockHttpResponse();
        when(httpResponse.hasResponseBody()).thenReturn(true);

        // when
        callback.onCompleted(httpResponse);

        // then
        verify(responseHandler).deserializeResponse(isNotNull());

        final ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(responseHandler).failed(captor.capture());

        assertSame(exception, captor.getValue());

    }

    private org.asynchttpclient.Response mockHttpResponse() {

        final org.asynchttpclient.Response httpResponse = mock(org.asynchttpclient.Response.class);
        final int expectedStatusCode = 1;
        final String expectedReason = UUID.randomUUID().toString();
        when(httpResponse.getStatusCode()).thenReturn(expectedStatusCode);
        when(httpResponse.getStatusText()).thenReturn(expectedReason);

        final InputStream inputStream = new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8));
        when(httpResponse.getResponseBodyAsStream()).thenReturn(inputStream);

        return httpResponse;

    }

}
