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

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.netty.buffer.ByteBuf;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.client.JestResultHandler;
import io.searchbox.core.Bulk;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.appenders.log4j2.elasticsearch.BufferedItemSource;
import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.NoopFailoverPolicy;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactoryTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Function;

import static org.appenders.log4j2.elasticsearch.BufferedItemSourcePoolTest.byteBufAllocator;
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

public class BufferedJestHttpObjectFactoryTest {

    static {
        System.setProperty("io.netty.allocator.maxOrder", "1");
    }

    private static final int TEST_CONNECTION_TIMEOUT = 1111;
    private static final int TEST_READ_TIMEOUT = 2222;

    public static final String TEST_SERVER_URIS = "http://localhost:9200";
    private static final int TEST_MAX_TOTAL_CONNECTIONS = 11;
    private static final int TEST_DEFAULT_MAX_TOTAL_CONNECTIONS_PER_ROUTE = 22;
    private static final boolean TEST_DISCOVERY_ENABLED = true;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    public static BufferedJestHttpObjectFactory.Builder createTestObjectFactoryBuilder() {

        PooledItemSourceFactory bufferedSourceFactory = PooledItemSourceFactoryTest
                .createDefaultTestSourceFactoryConfig()
                .build();

        BufferedJestHttpObjectFactory.Builder builder = (BufferedJestHttpObjectFactory.Builder) BufferedJestHttpObjectFactory.newBuilder()
                .withItemSourceFactory(bufferedSourceFactory)
                .withServerUris(TEST_SERVER_URIS);
        return builder;
    }

    @Test
    public void builderThrowsIfSourceFactoryIsNotProvided() {

        // given
        BufferedJestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder();
        builder.withItemSourceFactory(null);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("No " + PooledItemSourceFactory.class.getSimpleName() + " configured");

        // when
        builder.build();

    }

    @Test(expected = ConfigurationException.class)
    public void builderFailsIfServerUrisStringIsNull() {

        // given
        BufferedJestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder();
        String serverUris = null;

        // when
        builder.withServerUris(serverUris);
        builder.build();

    }

    @Test
    public void configReturnsACopyOfServerUrisList() {

        // given
        BufferedJestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder();
        builder.withServerUris("http://localhost:9200;http://localhost:9201;http://localhost:9202");
        ClientObjectFactory<JestClient, Bulk> config = builder.build();

        // when
        Collection<String> serverUrisList = config.getServerList();
        serverUrisList.add("test");

        // then
        assertNotEquals(serverUrisList, config.getServerList());

    }


    @Test
    public void createsBufferedBulkOperationsByDefault() {

        // given
        BufferedJestHttpObjectFactory factory = spy(createTestObjectFactoryBuilder().build());

        // when
        BatchOperations<Bulk> batchOperation = factory.createBatchOperations();

        // then
        assertEquals(BufferedBulkOperations.class, batchOperation.getClass());

    }

    @Test
    public void clientIsInitializedOnlyOnce() {

        // given
        BufferedJestHttpObjectFactory factory = spy(createTestObjectFactoryBuilder().build());

        // when
        JestClient client1 = factory.createClient();
        JestClient client2 = factory.createClient();

        // then
        assertEquals(client1, client2);

    }

    @Test
    public void httpParamsArePassedToCreatedObject() throws IllegalArgumentException, IllegalAccessException {

        // given
        BufferedJestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder();
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
    public void authIsAppliedIfConfigured() {

        // given
        Auth auth = mock(Auth.class);

        BufferedJestHttpObjectFactory factory = (BufferedJestHttpObjectFactory) createTestObjectFactoryBuilder()
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
        BufferedJestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder();
        ClientObjectFactory<JestClient, Bulk> config = builder.build();

        FailoverPolicy failoverPolicy = Mockito.spy(new NoopFailoverPolicy());

        String payload1 = "test1";
        String payload2 = "test2";
        ItemSource<ByteBuf> source1 = createDefaultTestBuffereItemSource(payload1);
        ItemSource<ByteBuf> source2 = createDefaultTestBuffereItemSource(payload2);
        Bulk bulk = createTestBatch(source1, source2);

        // when
        config.createFailureHandler(failoverPolicy).apply(bulk);

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(failoverPolicy, times(2)).deliver(captor.capture());

        assertTrue(captor.getAllValues().contains(payload1));
        assertTrue(captor.getAllValues().contains(payload2));
    }

    @Test
    public void clientIsCalledWhenListenerIsNotified() {

        // given
        BufferedJestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder();
        ClientObjectFactory<JestClient, Bulk> config = spy(builder.build());

        JestClient mockedJestClient = mock(JestClient.class);
        when(config.createClient()).thenReturn(mockedJestClient);

        FailoverPolicy failoverPolicy = spy(new NoopFailoverPolicy());
        Function<Bulk, Boolean> listener = config.createBatchListener(failoverPolicy);

        ItemSource<ByteBuf> payload1 = createDefaultTestBuffereItemSource("test1");
        ItemSource<ByteBuf> payload2 = createDefaultTestBuffereItemSource("test2");
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
        BufferedJestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder();
        BufferedJestHttpObjectFactory config = spy(builder.build());

        ItemSource<ByteBuf> payload1 = createDefaultTestBuffereItemSource("test1");
        ItemSource<ByteBuf> payload2 = createDefaultTestBuffereItemSource("test2");
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
        verify((BufferedBulk)bulk, times(1)).completed();

        assertEquals(bulk, captor.getValue());
    }

    @Test
    public void failoverIsNotExecutedAfterSuccessfulRequest() {

        // given
        BufferedJestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder();
        BufferedJestHttpObjectFactory config = spy(builder.build());

        ItemSource<ByteBuf> payload1 = createDefaultTestBuffereItemSource("test1");
        ItemSource<ByteBuf> payload2 = createDefaultTestBuffereItemSource("test2");
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
        BufferedJestHttpObjectFactory.Builder builder = createTestObjectFactoryBuilder();
        BufferedJestHttpObjectFactory config = spy(builder.build());

        ItemSource<ByteBuf> payload1 = createDefaultTestBuffereItemSource("test1");
        ItemSource<ByteBuf> payload2 = createDefaultTestBuffereItemSource("test2");

        Bulk bulk = createTestBatch(payload1, payload2);

        Function<Bulk, Boolean> failoverHandler = mock(Function.class);
        JestResultHandler<JestResult> resultHandler = config.createResultHandler(bulk, failoverHandler);

        // when
        resultHandler.failed(new IOException());

        // then
        ArgumentCaptor<Bulk> captor = ArgumentCaptor.forClass(Bulk.class);
        verify(failoverHandler, times(1)).apply(captor.capture());
        verify((BufferedBulk)bulk, times(1)).completed();

        assertEquals(bulk, captor.getValue());
    }

    private ItemSource<ByteBuf> createDefaultTestBuffereItemSource(String payload) {
        ByteBuf buffer = byteBufAllocator.buffer(16);
        buffer.writeBytes(payload.getBytes());
        return new BufferedItemSource(buffer, source -> {
            // noop
        });
    }

    private Bulk createTestBatch(ItemSource<ByteBuf>... payloads) {
        BufferedBulk.Builder builder = spy(new BufferedBulk.Builder());
        builder.withBuffer(new BufferedItemSource(byteBufAllocator.buffer(32), source -> {}));
        builder.withObjectWriter(mock(ObjectWriter.class));
        builder.withObjectReader(mock(ObjectReader.class));

        for (ItemSource<ByteBuf> payload : payloads) {
            builder.addAction(spy(new BufferedIndex.Builder(payload)).build());
        }
        return spy(builder.build());
    }

}
