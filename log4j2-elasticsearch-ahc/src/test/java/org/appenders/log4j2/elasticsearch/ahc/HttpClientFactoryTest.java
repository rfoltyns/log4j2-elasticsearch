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

import org.appenders.log4j2.elasticsearch.ahc.discovery.AHCServiceDiscovery;
import org.appenders.log4j2.elasticsearch.ahc.discovery.ServiceDiscovery;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Realm;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.SslEngineFactory;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.appenders.log4j2.elasticsearch.ahc.HttpClientProviderTest.createDefaultTestClientProvider;
import static org.appenders.log4j2.elasticsearch.ahc.discovery.AHCServiceDiscoveryTest.createNonSchedulingServiceDiscovery;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HttpClientFactoryTest {

    private static final Random RANDOM = new Random();

    private static final Collection<String> TEST_SERVER_LIST = Collections.singletonList("http://localhost:9200");

    private static final int TEST_CONNECTION_TIMEOUT = RANDOM.nextInt(1000) + 10;
    private static final int TEST_READ_TIMEOUT = RANDOM.nextInt(1000) + 10;

    private static final int TEST_MAX_TOTAL_CONNECTIONS = RANDOM.nextInt(1000) + 10;
    private static final int TEST_IO_THREAD_COUNT = RANDOM.nextInt(1000) + 10;
    private static final boolean TEST_POOLED_RESPONSE_BUFFERS_ENABLED = true;
    private static final int TEST_POOLED_RESPONSE_BUFFERS_SIZE_IN_BYTES = 34;

    @Test
    public void toStringDoesNotPrintSensitiveInfo() {

        // given
        final HttpClientFactory.Builder builder = createDefaultTestHttpClientFactoryBuilder();

        final String randomString = UUID.randomUUID().toString();

        final Security security = SecurityTest.createDefaultTestSecurityBuilder()
                .withCredentials(new BasicCredentials(randomString, randomString))
                .withCertInfo(new PEMCertInfo(randomString, randomString, randomString, randomString))
                .build();

        builder.withServerList(TEST_SERVER_LIST)
                .withAuth(security);

        // when
        final String toString = builder.toString();

        // then
        assertThat(toString, not(containsString(randomString)));
        assertThat(toString, containsString("auth=true"));

    }

    @Test
    public void toStringDoesNotPrintFullServiceDiscoveryInfo() {

        // given
        final HttpClientFactory.Builder builder = createDefaultTestHttpClientFactoryBuilder();

        final ServiceDiscovery serviceDiscovery = createNonSchedulingServiceDiscovery(createDefaultTestClientProvider(), (client, callback) -> {});

        builder.withServerList(TEST_SERVER_LIST)
                .withServiceDiscovery(serviceDiscovery);

        // when
        final String toString = builder.toString();

        // then
        assertThat(toString, containsString("serviceDiscovery=true"));
        assertEquals(toString.indexOf("serviceDiscovery"), toString.lastIndexOf("serviceDiscovery"));

    }

    @Test
    public void builderSetsAllFields() {

        // given
        final HttpClientFactory.Builder builder = createDefaultTestHttpClientFactoryBuilder();

        final ServiceDiscovery serviceDiscovery = mock(ServiceDiscovery.class);
        final Realm realm = mock(Realm.class);
        final SslEngineFactory sslEngineFactory = mock(SslEngineFactory.class);


        builder.withServerList(TEST_SERVER_LIST)
                .withConnTimeout(TEST_CONNECTION_TIMEOUT)
                .withReadTimeout(TEST_READ_TIMEOUT)
                .withMaxTotalConnections(TEST_MAX_TOTAL_CONNECTIONS)
                .withIoThreadCount(TEST_IO_THREAD_COUNT)
                .withRealm(realm)
                .withSslEngineFactory(sslEngineFactory)
                .withGzipCompression(true)
                .withServiceDiscovery(serviceDiscovery);


        // when
        final HttpClientFactory httpClientFactory = builder.build();

        // then
        assertEquals(TEST_SERVER_LIST, httpClientFactory.serverList);
        assertEquals(TEST_CONNECTION_TIMEOUT, httpClientFactory.connTimeout);
        assertEquals(TEST_READ_TIMEOUT, httpClientFactory.readTimeout);
        assertEquals(TEST_MAX_TOTAL_CONNECTIONS, httpClientFactory.maxTotalConnections);
        assertEquals(TEST_IO_THREAD_COUNT, httpClientFactory.ioThreadCount);
        assertEquals(realm, httpClientFactory.realm);
        assertEquals(sslEngineFactory, httpClientFactory.sslEngineFactory);
        assertTrue(httpClientFactory.gzipCompression);
        assertEquals(serviceDiscovery, httpClientFactory.serviceDiscovery);

    }

    @Test
    public void builderSetsDefaultFields() {

        // given
        final HttpClientFactory.Builder builder = createDefaultTestHttpClientFactoryBuilder();

        // when
        final HttpClientFactory httpClientFactory = builder.build();

        // then
        assertNull(httpClientFactory.serviceDiscovery);

    }

    @Test
    public void builderAppliesAuthIfConfigured() {

        // given
        final HttpClientFactory.Builder builder = createDefaultTestHttpClientFactoryBuilder();

        final SslEngineFactory sslEngineFactory = mock(SslEngineFactory.class);

        final Security security = spy(SecurityTest.createDefaultTestSecurityBuilder().build());

        builder.withAuth(security).withSslEngineFactory(sslEngineFactory);

        // when
        final HttpClientFactory httpClientFactory = builder.build();

        // then
        verify(security).configure(builder);

        assertNotEquals(sslEngineFactory, httpClientFactory.sslEngineFactory);

    }

    @Test
    public void createInstanceConfiguresAsyncHttpClient() {

        // given
        final HttpClientFactory factory = spy(createDefaultTestHttpClientFactory());
        final HttpClient client = spy(factory.createConfiguredClient(any(), any(), any()));
        when(factory.createConfiguredClient(any(), any(), any())).thenReturn(client);

        final AsyncHttpClient httpAsyncClient = mock(AsyncHttpClient.class);
        when(factory.createAsyncHttpClient()).thenReturn(httpAsyncClient);

        // when
        factory.createInstance();

        // then
        verify(factory, times(1)).createAsyncHttpClient();

    }

    @Test
    public void configuresServerPoolUrlsFromGivenServiceDiscovery() throws Exception {

        // given
        final String expectedAddress = "http://expected:9234";

        final AHCServiceDiscovery<HttpClient> serviceDiscovery = new AHCServiceDiscovery<>(
                createDefaultTestClientProvider(),
                (client, callback) -> callback.onSuccess(Collections.singletonList(expectedAddress)),
                Integer.MAX_VALUE);

        final HttpClientFactory.Builder builder = createDefaultTestHttpClientFactoryBuilder()
                .withServerList(Collections.emptyList())
                .withServiceDiscovery(serviceDiscovery);

        final HttpClientFactory httpClientFactory = spy(builder.build());

        final String expectedPath = "test";
        final Request request = new GenericRequest("GET", expectedPath, null);

        final HttpClient httpClient = httpClientFactory.createInstance();
        serviceDiscovery.start();

        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));

        // when
        final RequestBuilder clientRequest = httpClient.createClientRequest(request);

        // then
        assertEquals(expectedAddress + "/" + expectedPath, clientRequest.build().getUri().toString());

    }

    private HttpClientFactory createDefaultTestHttpClientFactory() {
        return createDefaultTestHttpClientFactoryBuilder().build();
    }

    public static HttpClientFactory.Builder createDefaultTestHttpClientFactoryBuilder() {
        return new HttpClientFactory.Builder()
                .withServerList(TEST_SERVER_LIST)
                .withMaxTotalConnections(1);
    }

    public static class BuilderMatcher extends BaseMatcher<HttpClientFactory.Builder> {

        private final HttpClientFactory.Builder current;
        private HttpClientFactory.Builder other;

        public BuilderMatcher(final HttpClientFactory.Builder current) {
            this.current = current;
        }

        @Override
        public boolean matches(final Object item) {

            if (item == null) {
                return false;
            }

            if (!(item instanceof HttpClientFactory.Builder)) {
                return false;
            }

            other = (HttpClientFactory.Builder) item;

            if (!current.serverList.equals(other.serverList)) {
                return false;
            }

            if (current.readTimeout != other.readTimeout) {
                return false;
            }

            if (current.connTimeout != other.connTimeout) {
                return false;
            }

            if (current.maxTotalConnections != other.maxTotalConnections) {
                return false;
            }

            if (current.ioThreadCount != other.ioThreadCount) {
                return false;
            }

            if (current.auth != other.auth) {
                return false;
            }

            if (current.serviceDiscovery != other.serviceDiscovery) {
                return false;
            }

            return true;

        }

        @Override
        public void describeTo(final Description description) {
            description.appendText(current.toString());
        }
    }
}
