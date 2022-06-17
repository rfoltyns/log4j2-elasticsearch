package org.appenders.log4j2.elasticsearch.ahc.discovery;

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

import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.appenders.log4j2.elasticsearch.ahc.HttpClient;
import org.appenders.log4j2.elasticsearch.ahc.HttpClientProvider;
import org.appenders.log4j2.elasticsearch.metrics.MetricsRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.appenders.core.logging.InternalLogging.setLogger;
import static org.appenders.core.logging.InternalLoggingTest.mockTestLogger;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AHCServiceDiscoveryTest {

    public static AHCServiceDiscovery<HttpClient> createNonSchedulingServiceDiscovery(
            final HttpClientProvider clientProvider,
            final ServiceDiscoveryRequest<HttpClient> serviceDiscoveryRequest) {
        return new AHCServiceDiscovery<HttpClient>(
                clientProvider,
                serviceDiscoveryRequest,
                1000) {
            @Override
            void scheduleRefreshTask() {
            }
        };
    }

    // Keep private. Should never be used outside this test as it schedules on LifeCycle.start()
    private static AHCServiceDiscovery<HttpClient> createDefaultTestServiceDiscovery() {
        return createDefaultTestServiceDiscovery(clientProviderMock());
    }

    // Keep private. Should never be used outside this test as it schedules on LifeCycle.start()
    private static AHCServiceDiscovery<HttpClient> createDefaultTestServiceDiscovery(
            final HttpClientProvider clientProvider) {
        return createDefaultTestServiceDiscovery(clientProvider, new TestServiceDiscoveryRequest());
    }

    // Keep private. Should never be used outside this test as it schedules on LifeCycle.start()
    private static AHCServiceDiscovery<HttpClient> createDefaultTestServiceDiscovery(
            final HttpClientProvider clientProvider,
            final TestServiceDiscoveryRequest serviceDiscoveryRequest) {
        return new AHCServiceDiscovery<>(
                clientProvider,
                serviceDiscoveryRequest,
                1000);
    }

    @Test
    public void refreshThrowsIfNotStarted() {

        // given
        final AHCServiceDiscovery<HttpClient> serviceDiscovery = createDefaultTestServiceDiscovery(clientProviderMock());

        assertFalse(serviceDiscovery.isStarted());

        // when
        final IllegalStateException exception = assertThrows(IllegalStateException.class, serviceDiscovery::refresh);

        // then
        assertThat(exception.getMessage(), containsString(AHCServiceDiscovery.class.getSimpleName() + " not started"));

    }

    @Test
    public void refreshTaskLogsInternalExceptions() {

        // given
        final AHCServiceDiscovery<HttpClient> serviceDiscovery = spy(createDefaultTestServiceDiscovery(clientProviderMock()));

        final RuntimeException testException = new RuntimeException("testException");
        doThrow(testException).when(serviceDiscovery).refresh();

        final Runnable refreshServerList = serviceDiscovery.new RefreshServerList();

        final Logger logger = mockTestLogger();

        // when
        refreshServerList.run();

        // then
        verify(logger).error(AHCServiceDiscovery.class.getSimpleName() + ": Unable to refresh addresses: testException", testException);

        setLogger(null);

    }

    @Test
    public void refreshTaskLogsClientExceptions() {

        // given
        final RuntimeException expectedException = new RuntimeException("testException");
        final AHCServiceDiscovery<HttpClient> serviceDiscovery = new AHCServiceDiscovery<HttpClient>(
                clientProviderMock(),
                (client, callback) -> {

                    callback.onFailure(expectedException);
                },
                Integer.MAX_VALUE) {
            @Override
            void scheduleRefreshTask() {
                // noop
            }
        };
        serviceDiscovery.start();

        final Runnable refreshServerList = serviceDiscovery.new RefreshServerList();

        final Logger logger = mockTestLogger();

        // when
        refreshServerList.run();

        // then
        verify(logger).error(AHCServiceDiscovery.class.getSimpleName() + ": Unable to refresh addresses: testException", expectedException);

        setLogger(null);

    }

    @Test
    public void refreshExecutesGivenServiceDiscoveryRequest() {

        // given
        final TestServiceDiscoveryRequest serviceDiscoveryRequest = mock(TestServiceDiscoveryRequest.class);
        final AHCServiceDiscovery<HttpClient> serviceDiscovery = createNonSchedulingServiceDiscovery(
                clientProviderMock(),
                serviceDiscoveryRequest);

        assertFalse(serviceDiscovery.isStarted());

        serviceDiscovery.start();

        // when
        serviceDiscovery.refresh();

        // then
        verify(serviceDiscoveryRequest).execute(any(), any());

    }

    @Test
    public void refreshNotifiesListenersOnAddedAddress() {

        // given
        final String expectedAddress1 = "http://localhost:9200";
        final String expectedAddress2 = "http://localhost:9201";

        final List<String> response1 = Collections.singletonList(expectedAddress1);
        final List<String> response2 = Arrays.asList(expectedAddress1, expectedAddress2);

        final AHCServiceDiscovery<HttpClient> serviceDiscovery = createNonSchedulingServiceDiscovery(
                clientProviderMock(),
                new TestServiceDiscoveryRequest(Arrays.asList(response1, response2)));


        final ServerInfoListener listener1 = mock(ServerInfoListener.class);
        serviceDiscovery.addListener(listener1);

        final ServerInfoListener listener2 = mock(ServerInfoListener.class);
        serviceDiscovery.addListener(listener2);

        serviceDiscovery.start();

        // when
        serviceDiscovery.refresh();
        serviceDiscovery.refresh();

        // then
        // then
        @SuppressWarnings("unchecked") final ArgumentCaptor<List<ServerInfo>> captor1 = forClass(List.class);
        verify(listener1, times(2)).onServerInfo(captor1.capture());

        @SuppressWarnings("unchecked") final ArgumentCaptor<List<ServerInfo>> captor2 = forClass(List.class);
        verify(listener2, times(2)).onServerInfo(captor2.capture());

        assertEquals(captor1.getAllValues(), captor2.getAllValues());

        assertEquals(1, captor1.getAllValues().get(0).size());
        assertEquals(expectedAddress1, captor1.getAllValues().get(0).get(0).getResolvedAddress());

        assertEquals(2, captor1.getAllValues().get(1).size());
        assertEquals(expectedAddress1, captor1.getAllValues().get(1).get(0).getResolvedAddress());
        assertEquals(expectedAddress2, captor1.getAllValues().get(1).get(1).getResolvedAddress());

    }

    @Test
    public void refreshNotifiesListenersOnReplacedAddress() {

        // given
        final String expectedAddress1 = "http://localhost:9200";
        final String expectedAddress2 = "http://localhost:9201";
        final String expectedAddress3 = "http://localhost:9202";

        final List<String> response1 = Arrays.asList(expectedAddress1, expectedAddress2);
        final List<String> response2 = Arrays.asList(expectedAddress1, expectedAddress3);

        final AHCServiceDiscovery<HttpClient> serviceDiscovery = createNonSchedulingServiceDiscovery(
                clientProviderMock(),
                new TestServiceDiscoveryRequest(Arrays.asList(response1, response2)));

        final ServerInfoListener listener1 = mock(ServerInfoListener.class);
        serviceDiscovery.addListener(listener1);

        final ServerInfoListener listener2 = mock(ServerInfoListener.class);
        serviceDiscovery.addListener(listener2);

        serviceDiscovery.start();

        // when
        serviceDiscovery.refresh();
        serviceDiscovery.refresh();

        // then
        @SuppressWarnings("unchecked") final ArgumentCaptor<List<ServerInfo>> captor1 = forClass(List.class);
        verify(listener1, times(2)).onServerInfo(captor1.capture());

        @SuppressWarnings("unchecked") final ArgumentCaptor<List<ServerInfo>> captor2 = forClass(List.class);
        verify(listener2, times(2)).onServerInfo(captor2.capture());

        assertEquals(captor1.getAllValues(), captor2.getAllValues());

        assertEquals(2, captor1.getAllValues().get(0).size());
        assertEquals(expectedAddress1, captor1.getAllValues().get(0).get(0).getResolvedAddress());
        assertEquals(expectedAddress2, captor1.getAllValues().get(0).get(1).getResolvedAddress());

        assertEquals(2, captor1.getAllValues().get(1).size());
        assertEquals(expectedAddress1, captor1.getAllValues().get(1).get(0).getResolvedAddress());
        assertEquals(expectedAddress3, captor1.getAllValues().get(1).get(1).getResolvedAddress());

    }

    @Test
    public void refreshNotifiesListenersOnRemovedAddress() {

        // given
        final String expectedAddress1 = "http://localhost:9200";
        final String expectedAddress2 = "http://localhost:9201";

        final List<String> response1 = Arrays.asList(expectedAddress1, expectedAddress2);
        final List<String> response2 = Collections.singletonList(expectedAddress1);

        final AHCServiceDiscovery<HttpClient> serviceDiscovery = createNonSchedulingServiceDiscovery(
                clientProviderMock(),
                new TestServiceDiscoveryRequest(Arrays.asList(response1, response2)));

        final ServerInfoListener listener1 = mock(ServerInfoListener.class);
        serviceDiscovery.addListener(listener1);

        final ServerInfoListener listener2 = mock(ServerInfoListener.class);
        serviceDiscovery.addListener(listener2);

        serviceDiscovery.start();

        // when
        serviceDiscovery.refresh();
        serviceDiscovery.refresh();

        // then
        @SuppressWarnings("unchecked") final ArgumentCaptor<List<ServerInfo>> captor1 = forClass(List.class);
        verify(listener1, times(2)).onServerInfo(captor1.capture());

        @SuppressWarnings("unchecked") final ArgumentCaptor<List<ServerInfo>> captor2 = forClass(List.class);
        verify(listener2, times(2)).onServerInfo(captor2.capture());

        assertEquals(captor1.getAllValues(), captor2.getAllValues());
        assertEquals(2, captor1.getAllValues().size());

        assertEquals(2, captor1.getAllValues().get(0).size());
        assertEquals(expectedAddress1, captor1.getAllValues().get(0).get(0).getResolvedAddress());
        assertEquals(expectedAddress2, captor1.getAllValues().get(0).get(1).getResolvedAddress());

        assertEquals(1, captor1.getAllValues().get(1).size());
        assertEquals(expectedAddress1, captor1.getAllValues().get(1).get(0).getResolvedAddress());

    }

    @Test
    public void refreshDoesNotNotifyListenersWhenAddressListHasNotChanged() {

        // given
        final String expectedAddress1 = "http://localhost:9200";
        final String expectedAddress2 = "http://localhost:9201";

        final List<String> response1 = Arrays.asList(expectedAddress1, expectedAddress2);
        final List<String> response2 = Arrays.asList(expectedAddress2, expectedAddress1);

        final AHCServiceDiscovery<HttpClient> serviceDiscovery = createNonSchedulingServiceDiscovery(
                clientProviderMock(),
                new TestServiceDiscoveryRequest(Arrays.asList(response1, response2)));

        final ServerInfoListener listener1 = mock(ServerInfoListener.class);
        serviceDiscovery.addListener(listener1);

        final ServerInfoListener listener2 = mock(ServerInfoListener.class);
        serviceDiscovery.addListener(listener2);

        serviceDiscovery.start();

        // when
        serviceDiscovery.refresh();
        serviceDiscovery.refresh();

        // then
        @SuppressWarnings("unchecked") final ArgumentCaptor<List<ServerInfo>> captor1 = forClass(List.class);
        verify(listener1).onServerInfo(captor1.capture());

        @SuppressWarnings("unchecked") final ArgumentCaptor<List<ServerInfo>> captor2 = forClass(List.class);
        verify(listener2).onServerInfo(captor2.capture());

        assertEquals(captor1.getAllValues(), captor2.getAllValues());

        assertEquals(expectedAddress1, captor1.getAllValues().get(0).get(0).getResolvedAddress());
        assertEquals(expectedAddress2, captor1.getAllValues().get(0).get(1).getResolvedAddress());

    }

    @Test
    public void refreshNotifiesOldListenersWhenNewListenerAddedAndAddressListHasNotChanged() {

        // given
        final String expectedAddress1 = "http://localhost:9200";
        final String expectedAddress2 = "http://localhost:9201";

        final List<String> response1 = Arrays.asList(expectedAddress1, expectedAddress2);
        final List<String> response2 = Arrays.asList(expectedAddress2, expectedAddress1);

        final AHCServiceDiscovery<HttpClient> serviceDiscovery = createNonSchedulingServiceDiscovery(
                clientProviderMock(),
                new TestServiceDiscoveryRequest(Arrays.asList(response1, response2)));

        final ServerInfoListener listener1 = mock(ServerInfoListener.class);
        serviceDiscovery.addListener(listener1);

        serviceDiscovery.start();

        final ServerInfoListener listener2 = mock(ServerInfoListener.class);

        // when
        serviceDiscovery.refresh();

        serviceDiscovery.addListener(listener2);

        serviceDiscovery.refresh();

        // then
        @SuppressWarnings("unchecked") final ArgumentCaptor<List<ServerInfo>> captor1 = forClass(List.class);
        verify(listener1, times(2)).onServerInfo(captor1.capture());

        @SuppressWarnings("unchecked") final ArgumentCaptor<List<ServerInfo>> captor2 = forClass(List.class);
        verify(listener2).onServerInfo(captor2.capture());

        final List<List<ServerInfo>> captor1Values = captor1.getAllValues();
        assertEquals(captor1Values.get(1), captor2.getAllValues().get(0));

        assertEquals(captor1Values.get(0).get(0).getResolvedAddress(), captor1Values.get(1).get(1).getResolvedAddress());
        assertEquals(captor1Values.get(0).get(1).getResolvedAddress(), captor1Values.get(1).get(0).getResolvedAddress());

        assertEquals(expectedAddress1, captor1Values.get(0).get(0).getResolvedAddress());
        assertEquals(expectedAddress2, captor1Values.get(0).get(1).getResolvedAddress());

    }

    @Test
    public void refreshDoesNotNotifyListenerWhenNoAddressFound() {

        // given
        final AHCServiceDiscovery<HttpClient> serviceDiscovery = createNonSchedulingServiceDiscovery(
                clientProviderMock(),
                new TestServiceDiscoveryRequest() {
                    @Override
                    public void execute(final HttpClient client, final ServiceDiscoveryCallback<List<String>> callback) {
                        callback.onSuccess(Collections.emptyList());
                    }
                });

        final ServerInfoListener listener1 = mock(ServerInfoListener.class);
        serviceDiscovery.addListener(listener1);

        serviceDiscovery.start();

        // when
        serviceDiscovery.refresh();

        // then
        @SuppressWarnings("unchecked") final ArgumentCaptor<List<ServerInfo>> captor1 = forClass(List.class);
        verify(listener1, never()).onServerInfo(captor1.capture());

    }

    // =========
    // LIFECYCLE
    // =========

    @Test
    public void lifecycleStart() {

        // given
        final LifeCycle lifeCycle = createLifeCycleTestObject();

        assertTrue(lifeCycle.isStopped());

        // when
        lifeCycle.start();

        // then
        assertFalse(lifeCycle.isStopped());
        assertTrue(lifeCycle.isStarted());

    }

    @Test
    public void lifecycleStop() {

        // given
        final LifeCycle lifeCycle = createLifeCycleTestObject();

        assertTrue(lifeCycle.isStopped());

        lifeCycle.start();
        assertTrue(lifeCycle.isStarted());

        // when
        lifeCycle.stop();

        // then
        assertFalse(lifeCycle.isStarted());
        assertTrue(lifeCycle.isStopped());

    }

    @Test
    public void lifecycleStartStartsClientProvider() {

        // given
        final HttpClientProvider clientProvider = clientProviderMock();

        final LifeCycle lifeCycle = createDefaultTestServiceDiscovery(clientProvider);

        assertTrue(lifeCycle.isStopped());

        // when
        lifeCycle.start();
        lifeCycle.start();

        // then
        verify(clientProvider).start();

    }

    @Test
    public void lifecycleStopStopsClientProvider() {

        // given
        final HttpClientProvider clientProvider = clientProviderMock();

        final LifeCycle lifeCycle = createDefaultTestServiceDiscovery(clientProvider);

        assertTrue(lifeCycle.isStopped());

        lifeCycle.start();
        assertTrue(lifeCycle.isStarted());

        // when
        lifeCycle.stop();
        lifeCycle.stop();

        // then
        verify(clientProvider).stop();

    }

    @Test
    public void lifecycleStopDeregistersMetrics() {

        // given
        final HttpClientProvider clientProvider = clientProviderMock();

        final LifeCycle lifeCycle = createDefaultTestServiceDiscovery(clientProvider);

        assertTrue(lifeCycle.isStopped());

        lifeCycle.start();
        assertTrue(lifeCycle.isStarted());

        // when
        lifeCycle.stop();
        lifeCycle.stop();

        // then
        verify(clientProvider).deregister();

    }

    // =======
    // METRICS
    // =======

    @Test
    public void registersComponentsMetrics() {

        // given
        final HttpClientProvider clientProvider = clientProviderMock();

        final AHCServiceDiscovery<HttpClient> serviceDiscovery = createDefaultTestServiceDiscovery(clientProvider);

        final MetricsRegistry registry = mock(MetricsRegistry.class);

        // when
        serviceDiscovery.register(registry);

        // then
        verify(clientProvider).register(registry);

    }

    @Test
    public void deregistersComponentsMetrics() {

        // given
        final HttpClientProvider clientProvider = clientProviderMock();

        final AHCServiceDiscovery<HttpClient> serviceDiscovery = createDefaultTestServiceDiscovery(clientProvider);

        final MetricsRegistry registry = mock(MetricsRegistry.class);
        serviceDiscovery.register(registry);

        // when
        serviceDiscovery.deregister();

        // then
        verify(clientProvider).deregister();

    }

    private LifeCycle createLifeCycleTestObject() {
        return createDefaultTestServiceDiscovery();
    }

    private static HttpClientProvider clientProviderMock() {
        return mock(HttpClientProvider.class);
    }

    private static class TestServiceDiscoveryRequest implements ServiceDiscoveryRequest<HttpClient> {

        private final Iterator<List<String>> responses;

        public TestServiceDiscoveryRequest() {
            this("http://localhost:9200");
        }

        public TestServiceDiscoveryRequest(final List<List<String>> responses) {
            this.responses = responses.iterator();
        }

        public TestServiceDiscoveryRequest(final String... responses) {
            this.responses = Collections.singletonList(Arrays.asList(responses)).iterator();
        }

        @Override
        public void execute(final HttpClient client, final ServiceDiscoveryCallback<List<String>> callback) {

            if (!responses.hasNext()) {
                throw new RuntimeException("Ran out of responses. Fix your test");
            }

            callback.onSuccess(responses.next());

        }
    }

}
