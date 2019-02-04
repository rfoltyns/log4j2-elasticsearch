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

import io.searchbox.client.config.HttpClientConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BufferedJestClientFactoryTest {

    @Test
    public void getObjectConfiguresAsyncHttpClient() {

        // given
        BufferedJestClientFactory factory = spy(createDefaultTestBufferedJestClientFactory());
        BufferedJestHttpClient client = spy(factory.createDefaultClient());
        when(factory.createDefaultClient()).thenReturn(client);

        CloseableHttpAsyncClient httpAsyncClient = mock(CloseableHttpAsyncClient.class);
        when(factory.createAsyncHttpClient(any())).thenReturn(httpAsyncClient);

        // when
        factory.getObject();

        // then
        verify(factory, atLeastOnce()).createAsyncHttpClient(any());
        verify(client).setAsyncClient(eq(httpAsyncClient));
        verify(httpAsyncClient).start();

    }

    @Test
    public void getObjectConfiguresHttpClient() {

        // given
        BufferedJestClientFactory factory = spy(createDefaultTestBufferedJestClientFactory());
        BufferedJestHttpClient client = spy(factory.createDefaultClient());
        when(factory.createDefaultClient()).thenReturn(client);

        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        when(factory.createHttpClient(any())).thenReturn(httpClient);

        // when
        factory.getObject();

        // then
        verify(factory, atLeastOnce()).createHttpClient(any());
        verify(client).setHttpClient(eq(httpClient));

    }

    @Test
    public void getObjectConfiguresNodeCheckerIfDiscoveryEnabled() {

        // given
        HttpClientConfig.Builder httpClientConfigBuilder = createDefaultTestHttpClientConfig();
        httpClientConfigBuilder.discoveryEnabled(true);

        BufferedJestClientFactory factory = spy(createDefaultTestBufferedJestClientFactory(httpClientConfigBuilder));

        BufferedJestHttpClient client = spy(factory.createDefaultClient());
        when(factory.createDefaultClient()).thenReturn(client);

        // when
        factory.getObject();

        // then
        verify(factory).createNodeChecker(eq(client), any());

    }

    @Test
    public void getObjectDoesNotConfigureNodeCheckerIfDiscoveryNotEnabled() {

        // given
        HttpClientConfig.Builder httpClientConfigBuilder = createDefaultTestHttpClientConfig();
        httpClientConfigBuilder.discoveryEnabled(false);

        BufferedJestClientFactory factory = spy(createDefaultTestBufferedJestClientFactory(httpClientConfigBuilder));

        BufferedJestHttpClient client = spy(factory.createDefaultClient());
        when(factory.createDefaultClient()).thenReturn(client);

        // when
        factory.getObject();

        // then
        verify(factory, never()).createNodeChecker(eq(client), any());

    }

    @Test
    public void getObjectConfiguresConnectionReaperIfMaxConnectionIdleTimeIsGreaterThanZero() {

        // given
        HttpClientConfig.Builder httpClientConfigBuilder = createDefaultTestHttpClientConfig();
        httpClientConfigBuilder.maxConnectionIdleTime(1000, TimeUnit.MILLISECONDS);

        BufferedJestClientFactory factory = spy(createDefaultTestBufferedJestClientFactory(httpClientConfigBuilder));

        BufferedJestHttpClient client = spy(factory.createDefaultClient());
        when(factory.createDefaultClient()).thenReturn(client);

        // when
        factory.getObject();

        // then
        verify(factory).createConnectionReaper(eq(client), any(), any());

    }

    @Test
    public void getObjectDoesNotConfigureConnectionReaperIfMaxConnectionIdleTimeIsZero() {

        // given
        HttpClientConfig.Builder httpClientConfigBuilder = createDefaultTestHttpClientConfig();
        httpClientConfigBuilder.maxConnectionIdleTime(0, TimeUnit.MILLISECONDS);

        BufferedJestClientFactory factory = spy(createDefaultTestBufferedJestClientFactory(httpClientConfigBuilder));

        BufferedJestHttpClient client = spy(factory.createDefaultClient());
        when(factory.createDefaultClient()).thenReturn(client);

        // when
        factory.getObject();

        // then
        verify(factory, never()).createConnectionReaper(eq(client), any(), any());

    }

    private BufferedJestClientFactory createDefaultTestBufferedJestClientFactory() {
        return createDefaultTestBufferedJestClientFactory(createDefaultTestHttpClientConfig());
    }

    private BufferedJestClientFactory createDefaultTestBufferedJestClientFactory(HttpClientConfig.Builder httpClientconfigBuilder) {
        BufferedJestClientFactory factory = new BufferedJestClientFactory(httpClientconfigBuilder.build());
//        factory.setHttpClientConfig(httpClientconfigBuilder.build());
        return factory;
    }

    private HttpClientConfig.Builder createDefaultTestHttpClientConfig() {
        return new HttpClientConfig.Builder("http://localhost:9200");
    }

}
