package org.appenders.log4j2.elasticsearch.jest;

/*-
 * #%L
 * %%
 * Copyright (C) 2017 Rafal Foltynski
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */



import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.client.JestResultHandler;
import io.searchbox.core.Bulk;
import io.searchbox.core.Index;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;
import org.appenders.log4j2.elasticsearch.NoopFailoverPolicy;
import org.appenders.log4j2.elasticsearch.jest.JestHttpObjectFactory.Builder;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JestHttpObjectFactoryTest {

    private static final int TEST_CONNECTION_TIMEOUT = 1111;
    private static final int TEST_READ_TIMEOUT = 2222;

    public static final String TEST_SERVER_URIS = "http://localhost:9200";
    private static final int TEST_MAX_TOTAL_CONNECTIONS = 11;
    private static final int TEST_DEFAULT_MAX_TOTAL_CONNECTIONS_PER_ROUTE = 22;
    private static final boolean TEST_DISCOVERY_ENABLED = true;

    public static Builder createTestObjectFactoryBuilder() {
        Builder builder = JestHttpObjectFactory.newBuilder();
        builder.withServerUris(TEST_SERVER_URIS);
        return builder;
    }

    @Test(expected = ConfigurationException.class)
    public void builderFailsIfServerUrisStringIsNull() {

        // given
        Builder builder = createTestObjectFactoryBuilder();
        String serverUris = null;

        // when
        builder.withServerUris(serverUris);
        builder.build();

    }

    @Test
    public void configReturnsACopyOfServerUrisList() {

        // given
        Builder builder = createTestObjectFactoryBuilder();
        builder.withServerUris("http://localhost:9200;http://localhost:9201;http://localhost:9202");
        ClientObjectFactory<JestClient, Bulk> config = builder.build();

        // when
        Collection<String> serverUrisList = config.getServerList();
        serverUrisList.add("test");

        // then
        assertNotEquals(serverUrisList, config.getServerList());

    }

    @Test
    public void clientIsInitializedOnlyOnce() {

        // given
        JestHttpObjectFactory factory = spy(createTestObjectFactoryBuilder().build());

        // when
        JestClient client1 = factory.createClient();
        JestClient client2 = factory.createClient();

        // then
        assertEquals(client1, client2);

    }

    @Test
    public void httpParamsArePassedToCreatedObject() throws IllegalArgumentException, IllegalAccessException {

        // given
        Builder builder = createTestObjectFactoryBuilder();
        builder.withConnTimeout(TEST_CONNECTION_TIMEOUT);
        builder.withReadTimeout(TEST_READ_TIMEOUT);
        builder.withMaxTotalConnection(TEST_MAX_TOTAL_CONNECTIONS);
        builder.withDefaultMaxTotalConnectionPerRoute(TEST_DEFAULT_MAX_TOTAL_CONNECTIONS_PER_ROUTE);
        builder.withDiscoveryEnabled(TEST_DISCOVERY_ENABLED);

        // when
        ClientObjectFactory<JestClient, Bulk> config = builder.build();

        // then
        assertEquals(TEST_CONNECTION_TIMEOUT,
                org.powermock.api.mockito.PowerMockito.field(config.getClass(), "connTimeout").get(config));
        assertEquals(TEST_READ_TIMEOUT,
                PowerMockito.field(config.getClass(), "readTimeout").get(config));
        assertEquals(TEST_MAX_TOTAL_CONNECTIONS,
                PowerMockito.field(config.getClass(), "maxTotalConnections").get(config));
        assertEquals(TEST_DEFAULT_MAX_TOTAL_CONNECTIONS_PER_ROUTE,
                PowerMockito.field(config.getClass(), "defaultMaxTotalConnectionsPerRoute").get(config));
        assertEquals(TEST_DISCOVERY_ENABLED,
                PowerMockito.field(config.getClass(), "discoveryEnabled").get(config));

    }

    @Test
    public void authIsAppliedIfCOnfigured() {

        // given
        Auth auth = mock(Auth.class);

        JestHttpObjectFactory factory = createTestObjectFactoryBuilder()
                .withAuth(auth)
                .build();

        // when
        factory.createClient();

        //then
        verify(auth).configure(any());
    }

    @Test
    public void failureHandlerExecutesFailoverForEachBatchItemSeparately() {

        // given
        Builder builder = createTestObjectFactoryBuilder();
        ClientObjectFactory<JestClient, Bulk> config = builder.build();

        FailoverPolicy failoverPolicy = Mockito.spy(new NoopFailoverPolicy());

        String payload1 = "test1";
        String payload2 = "test2";
        Bulk bulk = new Bulk.Builder()
                .addAction(spy(new Index.Builder(payload1)).build())
                .addAction(spy(new Index.Builder(payload2)).build())
                .build();

        // when
        config.createFailureHandler(failoverPolicy).apply(bulk);

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(failoverPolicy, times(2)).deliver((String) captor.capture());

        assertTrue(captor.getAllValues().contains(payload1));
        assertTrue(captor.getAllValues().contains(payload2));
    }

    @Test
    public void clientIsCalledWhenListenerIsNotified() {

        // given
        Builder builder = createTestObjectFactoryBuilder();
        ClientObjectFactory<JestClient, Bulk> config = spy(builder.build());

        JestClient mockedJestClient = mock(JestClient.class);
        when(config.createClient()).thenReturn(mockedJestClient);

        FailoverPolicy failoverPolicy = spy(new NoopFailoverPolicy());
        Function<Bulk, Boolean> listener = config.createBatchListener(failoverPolicy);

        String payload1 = "test1";
        String payload2 = "test2";
        Bulk bulk = createTestBatch(payload1, payload2);

        // when
        listener.apply(bulk);

        // then
        ArgumentCaptor<Bulk> captor = ArgumentCaptor.forClass(Bulk.class);
        verify(mockedJestClient, times(1)).executeAsync((Bulk) captor.capture(), Mockito.any());

        assertEquals(bulk, captor.getValue());
    }

    @Test
    public void failoverIsExecutedAfterNonSuccessfulRequest() {

        // given
        Builder builder = createTestObjectFactoryBuilder();
        JestHttpObjectFactory config = spy(builder.build());

        String payload1 = "test1";
        String payload2 = "test2";
        Bulk bulk = createTestBatch(payload1, payload2);

        Function<Bulk, Boolean> failoverHandler = mock(Function.class);
        JestResultHandler<JestResult> resultHandler = config.createResultHandler(bulk, failoverHandler);

        JestResult result = mock(JestResult.class);
        when(result.isSucceeded()).thenReturn(false);

        // when
        resultHandler.completed(result);

        // then
        ArgumentCaptor<Bulk> captor = ArgumentCaptor.forClass(Bulk.class);
        verify(failoverHandler, times(1)).apply(captor.capture());

        assertEquals(bulk, captor.getValue());
    }

    @Test
    public void failoverIsNotExecutedAfterSuccessfulRequest() {

        // given
        Builder builder = createTestObjectFactoryBuilder();
        JestHttpObjectFactory config = spy(builder.build());

        String payload1 = "test1";
        String payload2 = "test2";
        Bulk bulk = createTestBatch(payload1, payload2);

        Function<Bulk, Boolean> failoverHandler = mock(Function.class);
        JestResultHandler<JestResult> resultHandler = config.createResultHandler(bulk, failoverHandler);

        JestResult result = mock(JestResult.class);
        when(result.isSucceeded()).thenReturn(true);

        // when
        resultHandler.completed(result);

        // then
        verify(failoverHandler, never()).apply(Mockito.any(Bulk.class));
    }

    @Test
    public void failoverIsExecutedAfterFailedRequest() {

        // given
        Builder builder = createTestObjectFactoryBuilder();
        JestHttpObjectFactory config = spy(builder.build());

        String payload1 = "test1";
        String payload2 = "test2";
        Bulk bulk = createTestBatch(payload1, payload2);

        Function<Bulk, Boolean> failoverHandler = mock(Function.class);
        JestResultHandler<JestResult> resultHandler = config.createResultHandler(bulk, failoverHandler);

        // when
        resultHandler.failed(new IOException());

        // then
        ArgumentCaptor<Bulk> captor = ArgumentCaptor.forClass(Bulk.class);
        verify(failoverHandler, times(1)).apply(captor.capture());

        assertEquals(bulk, captor.getValue());
    }

    private Bulk createTestBatch(String... payloads) {
        io.searchbox.core.Bulk.Builder builder = spy(new Bulk.Builder());
        for (String payload : payloads) {
            builder.addAction(spy(new Index.Builder(payload)).build());
        }
        return builder.build();
    }
}
