package org.appenders.log4j2.elasticsearch.hc;

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
import org.appenders.log4j2.elasticsearch.ClientProvider;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class AdminOperationsTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void passesIndexTemplateToClient() {

        //given
        HCHttp factory = Mockito.spy(HCHttpTest.createDefaultHttpObjectFactoryBuilder().build());

        HttpClient httpClient = mockedHttpClient(factory);
        AtomicReference<ByteBuf> argCaptor = new AtomicReference<>();
        mockedResult(httpClient, true, argCaptor);

        IndexTemplate indexTemplate = spy(IndexTemplate.newBuilder()
                .withPath("classpath:indexTemplate.json")
                .withName("testName")
                .build());

        String expectedPayload = indexTemplate.getSource();

        // when
        factory.execute(indexTemplate);

        // then
        ArgumentCaptor<IndexTemplateRequest> requestArgumentCaptor = ArgumentCaptor.forClass(IndexTemplateRequest.class);
        verify(httpClient).execute(requestArgumentCaptor.capture(), any());

        assertEquals(argCaptor.get().toString(Charset.forName("UTF-8")), expectedPayload);

    }

    @Test
    public void errorMessageContainsExceptionMessageOnTemplateActionFailure() {

        //given
        HttpClient httpClient = mock(HttpClient.class);
        HCHttp factory = Mockito.spy(HCHttpTest.createDefaultHttpObjectFactoryBuilder().build());
        when(factory.createClient()).thenReturn(httpClient);
        ArgumentCaptor<BlockingResponseHandler> captor = ArgumentCaptor.forClass(BlockingResponseHandler.class);
        when(httpClient.execute(any(), captor.capture())).thenReturn(BatchResultTest.createTestBatchResult(false, null));

        IndexTemplate indexTemplate = spy(IndexTemplate.newBuilder()
                .withPath("classpath:indexTemplate-7.json")
                .withName("testName")
                .build());

        factory.start();
        factory.execute(indexTemplate);

        // when
        String message = UUID.randomUUID().toString();
        captor.getValue().failed(new IOException(message));

        // then
        assertTrue(message, captor.getValue().getResult().getErrorMessage().contains(message));

    }

    @Test
    public void errorMessageIsRetrievedIfTemplateActionNotSucceeded() throws IOException {

        //given
        HCHttp factory = Mockito.spy(HCHttpTest.createDefaultHttpObjectFactoryBuilder().build());

        HttpClient httpClient = mockedHttpClient(factory);

        Response responseMock = mockedResult(httpClient, false);

        IndexTemplate indexTemplate = spy(IndexTemplate.newBuilder()
                .withPath("classpath:indexTemplate.json")
                .withName("testName")
                .build());

        // when
        factory.execute(indexTemplate);

        // then
        verify(responseMock).getErrorMessage();

    }

    @Test
    public void errorMessageIsNotRetrievedIfTemplateActionHasSucceeded() throws IOException {

        //given
        HCHttp factory = Mockito.spy(HCHttpTest.createDefaultHttpObjectFactoryBuilder().build());

        HttpClient httpClient = mockedHttpClient(factory);

        Response responseMock = mockedResult(httpClient, true);

        IndexTemplate indexTemplate = spy(IndexTemplate.newBuilder()
                .withPath("classpath:indexTemplate.json")
                .withName("testName")
                .build());

        // when
        factory.execute(indexTemplate);

        // then
        verify(responseMock, never()).getErrorMessage();

    }

    @Test
    public void clientStartsInsideDeferredOperation() {

        HCHttp factory = Mockito.spy(HCHttpTest.createDefaultHttpObjectFactoryBuilder().build());
        HttpClient httpClient = mockedHttpClient(factory);
        when(factory.createClient()).thenReturn(httpClient);

        Function<BatchRequest, Boolean> batchListener = factory.createBatchListener(mock(FailoverPolicy.class));

        // when
        factory.start();
        batchListener.apply(mock(BatchRequest.class));

        // then
        verify(httpClient, times(1)).start();

    }

    private Response mockedResult(HttpClient httpClient, boolean isSucceeded) {
        return mockedResult(httpClient, isSucceeded, new AtomicReference<>());
    }

    private Response mockedResult(HttpClient httpClient, boolean isSucceeded, AtomicReference<ByteBuf> argCaptor) {
        BatchResult result = mock(BatchResult.class);
        when(httpClient.execute(any(), any())).thenAnswer(invocation -> {
            IndexTemplateRequest templateRequest = invocation
                    .getArgumentAt(0, IndexTemplateRequest.class);
            argCaptor.set(((ByteBuf)templateRequest.source).copy());
            return result;
        });
        when(result.getErrorMessage()).thenReturn("IndexTemplate not added");

        when(result.isSucceeded()).thenReturn(isSucceeded);

        return result;
    }

    private HttpClient mockedHttpClient(HCHttp factory) {
        ClientProvider clientProvider = mock(ClientProvider.class);
        when(factory.getClientProvider(any())).thenReturn(clientProvider);

        HttpClient httpClient = mock(HttpClient.class);
        when(clientProvider.createClient()).thenReturn(httpClient);
        return httpClient;
    }

}
