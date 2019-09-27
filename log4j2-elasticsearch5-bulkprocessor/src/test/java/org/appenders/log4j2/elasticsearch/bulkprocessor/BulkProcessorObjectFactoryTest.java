package org.appenders.log4j2.elasticsearch.bulkprocessor;

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


import org.apache.logging.log4j.core.config.ConfigurationException;
import org.appenders.log4j2.elasticsearch.BatchEmitter;
import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.ClientProvider;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.NoopFailoverPolicy;
import org.appenders.log4j2.elasticsearch.Operation;
import org.appenders.log4j2.elasticsearch.bulkprocessor.BulkProcessorObjectFactory.Builder;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkRequestIntrospector;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.LocalTransportAddress;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BulkProcessorObjectFactoryTest {

    public static final String TEST_SERVER_URIS = "http://localhost:9300";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();


    public static Builder createTestObjectFactoryBuilder() {
        Builder builder = BulkProcessorObjectFactory.newBuilder();
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
        ClientObjectFactory<TransportClient, BulkRequest> config = builder.build();

        // when
        Collection<String> serverUrisList = config.getServerList();
        serverUrisList.add("test");

        // then
        assertNotEquals(serverUrisList.size(), config.getServerList().size());

    }

    @Test
    public void clientIsInitializedOnlyOnce() {

        // given
        BulkProcessorObjectFactory factory = spy(createTestObjectFactoryBuilder().build());

        BulkProcessorObjectFactory.InsecureTransportClientProvider clientProvider =
                new BulkProcessorObjectFactory.InsecureTransportClientProvider();

        when(factory.getClientProvider()).thenReturn(spy(clientProvider));

        // when
        TransportClient client1 = factory.createClient();
        TransportClient client2 = factory.createClient();

        // then
        verify(factory, times(1)).getClientProvider();
        assertEquals(client1, client2);

    }

    @Test
    public void throwsIfUnknownHostWasProvided() {

        // given
        Builder builder = createTestObjectFactoryBuilder();

        builder.withServerUris("http://unknowntesthost:8080");
        BulkProcessorObjectFactory factory = builder.build();

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("unknowntesthost");

        // when
        factory.createClient();

    }

    @Test
    public void secureTransportIsSetupByDefaultWhenAuthIsConfigured() {

        // given
        Builder builder = createTestObjectFactoryBuilder();
        builder.withAuth(XPackAuthTest.createTestBuilder().build());

        BulkProcessorObjectFactory factory = builder.build();

        // when
        ClientProvider clientProvider = factory.getClientProvider();

        // then
        Assert.assertTrue(clientProvider instanceof SecureClientProvider);

    }

    @Test
    public void insecureTransportIsSetupByDefaultWhenAuthIsNotConfigured() {

        // given
        Builder builder = createTestObjectFactoryBuilder();
        builder.withAuth(null);

        BulkProcessorObjectFactory factory = builder.build();

        // when
        ClientProvider clientProvider = factory.getClientProvider();

        // then
        Assert.assertTrue(clientProvider instanceof BulkProcessorObjectFactory.InsecureTransportClientProvider);

    }

    @Test
    public void minimalSetupUsesClientProviderByDefault() {

        // given
        Builder builder = createTestObjectFactoryBuilder();
        builder.withAuth(null);

        BulkProcessorObjectFactory factory = spy(builder.build());

        // when
        factory.createClient();

        // then
        verify(factory).getClientProvider();

    }

    @Test
    public void failureHandlerExecutesFailoverForEachBatchItemSeparately() {

        // given
        Builder builder = createTestObjectFactoryBuilder();
        ClientObjectFactory<TransportClient, BulkRequest> config = builder.build();

        FailoverPolicy failoverPolicy = spy(new NoopFailoverPolicy());

        String payload1 = "test1";
        String payload2 = "test2";
        BulkRequest bulk = new BulkRequest()
                .add(spy(new IndexRequest().source(payload1, XContentType.CBOR)))
                .add(spy(new IndexRequest().source(payload2, XContentType.CBOR)));

        // when
        config.createFailureHandler(failoverPolicy).apply(bulk);

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(failoverPolicy, times(2)).deliver(captor.capture());

        assertTrue(captor.getAllValues().contains(payload1));
        assertTrue(captor.getAllValues().contains(payload2));
    }

    @Test
    public void clientIsCalledWhenBatchItemIsAdded() {

        // given
        Builder builder = createTestObjectFactoryBuilder();
        ClientObjectFactory<TransportClient, BulkRequest> config = spy(builder.build());

        Settings settings = Settings.builder().build();
        TransportClient client = spy(new PreBuiltTransportClient(settings));
        client.addTransportAddress(new LocalTransportAddress("1"));
        when(config.createClient()).thenReturn(client);

        FailoverPolicy failoverPolicy = spy(new NoopFailoverPolicy());

        BulkProcessorFactory bulkProcessorFactory = new BulkProcessorFactory();
        BatchEmitter batchEmitter = bulkProcessorFactory.createInstance(
                        1,
                        100,
                        config,
                        failoverPolicy);

        String payload1 = "test1";
        ActionRequest testRequest = createTestRequest(payload1);

        // when
        batchEmitter.add(testRequest);

        // then
        ArgumentCaptor<BulkRequest> captor = ArgumentCaptor.forClass(BulkRequest.class);
        verify(client, times(1)).bulk(captor.capture(), Mockito.any());

        assertEquals(payload1, new BulkRequestIntrospector().items(captor.getValue()).iterator().next());
    }

    @Test
    public void failoverIsExecutedAfterNonSuccessfulRequest() {

        // given
        Builder builder = createTestObjectFactoryBuilder();
        ClientObjectFactory<TransportClient, BulkRequest> config = spy(builder.build());

        FailoverPolicy failoverPolicy = spy(new NoopFailoverPolicy());
        Function handler = spy(config.createFailureHandler(failoverPolicy));
        when(config.createFailureHandler(any())).thenReturn(handler);

        Settings settings = Settings.builder().build();
        TransportClient client = spy(new PreBuiltTransportClient(settings));
        client.addTransportAddress(new LocalTransportAddress("1"));
        when(config.createClient()).thenReturn(client);

        BulkProcessorFactory bulkProcessorFactory = new BulkProcessorFactory();
        BatchEmitter batchEmitter = bulkProcessorFactory.createInstance(
                1,
                100,
                config,
                failoverPolicy);

        String payload1 = "test1";
        ActionRequest testRequest = createTestRequest(payload1);

        // when
        batchEmitter.add(testRequest);

        // then
        ArgumentCaptor<BulkRequest> captor = ArgumentCaptor.forClass(BulkRequest.class);
        verify(handler, times(1)).apply(captor.capture());

        assertEquals(payload1, new BulkRequestIntrospector().items(captor.getValue()).iterator().next());
    }

    @Test
    public void throwsOnExecuteTemplateFailure() {

        //given
        BulkProcessorObjectFactory factory = spy(createTestObjectFactoryBuilder().build());

        String expectedMessage = "test-exception";

        when(factory.getClientProvider()).thenAnswer((Answer) invocation -> {
            throw new IOException(expectedMessage);
        });

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage(expectedMessage);

        // when
        factory.execute(mock(IndexTemplate.class));

    }

    @Test
    public void defaultBatchListenerDoesntThrow() {

        // given
        BulkProcessorObjectFactory factory = createTestObjectFactoryBuilder().build();
        Function<BulkRequest, Boolean> batchListener = factory.createBatchListener(failedPayload -> {
            throw new ConfigurationException("test exception");
        });

        // when
        batchListener.apply(null);

    }

    @Test
    public void addAdminOperationExecutesImediately() throws Exception {

        // given
        BulkProcessorObjectFactory factory = createTestObjectFactoryBuilder().build();

        Operation operation = mock(Operation.class);

        // when
        factory.addOperation(operation);

        // then
        verify(operation).execute();

    }

    @Test
    public void addAdminOperationExceptinsAreNotRethrown() throws Exception {

        // given
        BulkProcessorObjectFactory factory = createTestObjectFactoryBuilder().build();

        Operation operation = spy(new Operation() {
            @Override
            public void execute() throws Exception {
                throw new Exception("test exception");
            }
        });

        // when
        factory.addOperation(operation);

        // then
        verify(operation).execute();

    }

    private ActionRequest createTestRequest(String payload) {
        return spy(new IndexRequest().source(payload, XContentType.CBOR));
    }

}
