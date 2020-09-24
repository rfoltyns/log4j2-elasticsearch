package org.appenders.log4j2.elasticsearch.jest;

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

import io.searchbox.client.config.HttpClientConfig;
import org.apache.http.HttpHost;
import org.apache.http.config.Registry;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.reactor.IOReactorException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ExtendedJestClientFactoryTest {

    private Random random = new Random();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void ioReactorConfigUsesGivenIoThreadCount() {

        // given
        WrappedHttpClientConfig.Builder builder = createDefaultTestWrappedHttpClientConfigBuilder();

        int expectedIoThreadCount = random.nextInt(16) + 1;
        builder.ioThreadCount(expectedIoThreadCount);

        ExtendedJestClientFactory factory = new ExtendedJestClientFactory(builder.build());

        // when
        IOReactorConfig ioReactorConfig = factory.createIoReactorConfig();

        // then
        assertEquals(expectedIoThreadCount, ioReactorConfig.getIoThreadCount());

    }

    @Test
    public void ioReactorConfigUsesGivenReadTimeout() {

        // given
        HttpClientConfig.Builder httpClientConfigBuilder = createTestHttpClientConfigBuilder();

        int expectedReadTImeout = random.nextInt(1000) + 1;
        httpClientConfigBuilder.readTimeout(expectedReadTImeout);

        WrappedHttpClientConfig.Builder builder = createDefaultTestWrappedHttpClientConfigBuilder(httpClientConfigBuilder.build());
        ExtendedJestClientFactory factory = new ExtendedJestClientFactory(builder.build());

        // when
        IOReactorConfig ioReactorConfig = factory.createIoReactorConfig();

        // then
        assertEquals(expectedReadTImeout, ioReactorConfig.getSoTimeout());

    }

    @Test
    public void ioReactorConfigUsesGivenConnectTimeout() {

        // given
        HttpClientConfig.Builder httpClientConfigBuilder = createTestHttpClientConfigBuilder();

        int expectedConnectTImeout = random.nextInt(1000) + 1;
        httpClientConfigBuilder.connTimeout(expectedConnectTImeout);

        WrappedHttpClientConfig.Builder builder = createDefaultTestWrappedHttpClientConfigBuilder(httpClientConfigBuilder.build());
        ExtendedJestClientFactory factory = new ExtendedJestClientFactory(builder.build());

        // when
        IOReactorConfig ioReactorConfig = factory.createIoReactorConfig();

        // then
        assertEquals(expectedConnectTImeout, ioReactorConfig.getConnectTimeout());

    }


    @Test
    public void ioReactorUsesProvidedIOReactorConfig() throws IOReactorException {

        // given
        WrappedHttpClientConfig.Builder builder = createDefaultTestWrappedHttpClientConfigBuilder();
        ExtendedJestClientFactory factory = spy(new ExtendedJestClientFactory(builder.build()));

        // when
        factory.createIOReactor();

        // then
        verify(factory).createIoReactorConfig();

    }

    @Test
    public void registryBuilderUsesProvidedHttpIOSessionStrategy() {

        // given
        HttpClientConfig.Builder httpClientConfigBuilder = createTestHttpClientConfigBuilder();

        SchemeIOSessionStrategy expectedSchemeIOSessionStrategy = mock(SchemeIOSessionStrategy.class);
        httpClientConfigBuilder.httpIOSessionStrategy(expectedSchemeIOSessionStrategy);

        WrappedHttpClientConfig.Builder builder = createDefaultTestWrappedHttpClientConfigBuilder(httpClientConfigBuilder.build());
        ExtendedJestClientFactory factory = new ExtendedJestClientFactory(builder.build());

        // when
        Registry<SchemeIOSessionStrategy> registry = factory.createSchemeIOSessionStrategyRegistry();

        // then
        assertEquals(expectedSchemeIOSessionStrategy, registry.lookup("http"));

    }

    @Test
    public void registryBuilderUsesProvidedHttpsIOSessionStrategy() {

        // given
        SchemeIOSessionStrategy expectedSchemeIOSessionStrategy = mock(SchemeIOSessionStrategy.class);
        HttpClientConfig.Builder httpClientConfigBuilder = createTestHttpClientConfigBuilder(expectedSchemeIOSessionStrategy);

        WrappedHttpClientConfig.Builder builder = createDefaultTestWrappedHttpClientConfigBuilder(httpClientConfigBuilder.build());
        ExtendedJestClientFactory factory = new ExtendedJestClientFactory(builder.build());

        // when
        Registry<SchemeIOSessionStrategy> registry = factory.createSchemeIOSessionStrategyRegistry();

        // then
        assertEquals(expectedSchemeIOSessionStrategy, registry.lookup("https"));

    }

    @Test
    public void unconfiguredNHttpConnectionManagerUsesIOReactor() throws IOReactorException {

        // given
        WrappedHttpClientConfig.Builder builder = createDefaultTestWrappedHttpClientConfigBuilder();
        ExtendedJestClientFactory factory = spy(new ExtendedJestClientFactory(builder.build()));

        // when
        factory.createUnconfiguredPoolingNHttpClientConnectionManager();

        // then
        verify(factory).createIOReactor();

    }

    @Test
    public void unconfiguredNHttpConnectionManagerUsesSchemeIOSessionStrategy() {

        // given
        WrappedHttpClientConfig.Builder builder = createDefaultTestWrappedHttpClientConfigBuilder();
        ExtendedJestClientFactory factory = spy(new ExtendedJestClientFactory(builder.build()));

        // when
        factory.createUnconfiguredPoolingNHttpClientConnectionManager();

        // then
        verify(factory).createSchemeIOSessionStrategyRegistry();

    }

    @Test
    public void unconfiguredNHttpConnectionManagerInitRethrowsISEOnIOReactorException() throws IOReactorException {

        // given
        WrappedHttpClientConfig.Builder builder = createDefaultTestWrappedHttpClientConfigBuilder();
        ExtendedJestClientFactory factory = spy(new ExtendedJestClientFactory(builder.build()));

        String expectedMessage = UUID.randomUUID().toString();
        when(factory.createIOReactor()).thenThrow(new IOReactorException(expectedMessage));

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(expectedMessage);

        // when
        factory.createUnconfiguredPoolingNHttpClientConnectionManager();

        // then
        verify(factory).createSchemeIOSessionStrategyRegistry();

    }

    @Test
    public void getAsyncConnectionManagerUsesProvidedUnconiguredNHttpConnManager() {

        // given
        WrappedHttpClientConfig.Builder builder = createDefaultTestWrappedHttpClientConfigBuilder();
        ExtendedJestClientFactory factory = spy(new ExtendedJestClientFactory(builder.build()));

        // when
        factory.getAsyncConnectionManager();

        // then
        verify(factory).createUnconfiguredPoolingNHttpClientConnectionManager();

    }

    @Test
    public void getAsyncConnectionManagerConfiguresMaxTotalIfConfigured() {

        // given
        HttpClientConfig.Builder httpClientConfigBuilder = createTestHttpClientConfigBuilder();

        int expectedMaxTotalConnection = random.nextInt(100) + 10;
        httpClientConfigBuilder.maxTotalConnection(expectedMaxTotalConnection);

        WrappedHttpClientConfig.Builder builder = createDefaultTestWrappedHttpClientConfigBuilder(httpClientConfigBuilder.build());
        ExtendedJestClientFactory factory = spy(new ExtendedJestClientFactory(builder.build()));

        PoolingNHttpClientConnectionManager mockedNHttpConnectionManager = mock(PoolingNHttpClientConnectionManager.class);
        when(factory.createUnconfiguredPoolingNHttpClientConnectionManager())
                .thenReturn(mockedNHttpConnectionManager);

        // when
        factory.getAsyncConnectionManager();

        // then
        verify(mockedNHttpConnectionManager).setMaxTotal(eq(expectedMaxTotalConnection));

    }

    @Test
    public void getAsyncConnectionManagerConfiguresDefaultMaxTotalPerRouteIfConfigured() {

        // given
        HttpClientConfig.Builder httpClientConfigBuilder = createTestHttpClientConfigBuilder();

        int expectedMaxTotalConnection = random.nextInt(100) + 10;
        httpClientConfigBuilder.defaultMaxTotalConnectionPerRoute(expectedMaxTotalConnection);

        WrappedHttpClientConfig.Builder builder = createDefaultTestWrappedHttpClientConfigBuilder(httpClientConfigBuilder.build());
        ExtendedJestClientFactory factory = spy(new ExtendedJestClientFactory(builder.build()));

        PoolingNHttpClientConnectionManager mockedNHttpConnectionManager = mock(PoolingNHttpClientConnectionManager.class);
        when(factory.createUnconfiguredPoolingNHttpClientConnectionManager())
                .thenReturn(mockedNHttpConnectionManager);

        // when
        factory.getAsyncConnectionManager();

        // then
        verify(mockedNHttpConnectionManager).setDefaultMaxPerRoute(eq(expectedMaxTotalConnection));

    }

    @Test
    public void getAsyncConnectionManagerConfiguresMaxTotalPerRouteIfConfigured() {

        // given
        HttpClientConfig.Builder httpClientConfigBuilder = createTestHttpClientConfigBuilder();

        HttpRoute expectedHttpRoute = new HttpRoute(new HttpHost("localhost"));
        int expectedMaxTotalConnection = random.nextInt(100) + 10;
        httpClientConfigBuilder.maxTotalConnectionPerRoute(expectedHttpRoute, expectedMaxTotalConnection);

        WrappedHttpClientConfig.Builder builder = createDefaultTestWrappedHttpClientConfigBuilder(httpClientConfigBuilder.build());
        ExtendedJestClientFactory factory = spy(new ExtendedJestClientFactory(builder.build()));

        PoolingNHttpClientConnectionManager mockedNHttpConnectionManager = mock(PoolingNHttpClientConnectionManager.class);
        when(factory.createUnconfiguredPoolingNHttpClientConnectionManager())
                .thenReturn(mockedNHttpConnectionManager);

        // when
        factory.getAsyncConnectionManager();

        // then
        verify(mockedNHttpConnectionManager).setMaxPerRoute(eq(expectedHttpRoute), eq(expectedMaxTotalConnection));

    }

    private HttpClientConfig.Builder createTestHttpClientConfigBuilder() {
        return new HttpClientConfig.Builder("http://localhost:9200")
                .httpIOSessionStrategy(mock(SchemeIOSessionStrategy.class))
                .httpsIOSessionStrategy(mock(SchemeIOSessionStrategy.class));
    }

    private HttpClientConfig.Builder createTestHttpClientConfigBuilder(SchemeIOSessionStrategy schemeIOSessionStrategy) {
        return new HttpClientConfig.Builder("http://localhost:9200")
                .httpIOSessionStrategy(schemeIOSessionStrategy)
                .httpsIOSessionStrategy(schemeIOSessionStrategy);
    }

    private WrappedHttpClientConfig.Builder createDefaultTestWrappedHttpClientConfigBuilder() {
        return new WrappedHttpClientConfig.Builder(createTestHttpClientConfigBuilder().build());
    }

    private WrappedHttpClientConfig.Builder createDefaultTestWrappedHttpClientConfigBuilder(HttpClientConfig httpClientConfig) {
        return new WrappedHttpClientConfig.Builder(httpClientConfig);
    }

}
