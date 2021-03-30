package org.appenders.log4j2.elasticsearch.hc.discovery;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2020 Rafal Foltynski
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

import org.appenders.log4j2.elasticsearch.hc.BlockingResponseHandler;
import org.appenders.log4j2.elasticsearch.hc.HttpClient;
import org.appenders.log4j2.elasticsearch.hc.Request;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsIterableContaining.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ElasticsearchNodesQueryTest {

    @Test
    public void notifiesCallbackWithResultIfResponseSucceeded() {

        // given
        String expectedScheme = UUID.randomUUID().toString();
        ElasticsearchNodesQuery query = createDefaultTestQuery(expectedScheme);

        HttpClient httpClient = mock(HttpClient.class);
        ServiceDiscoveryCallback<List<String>> callback = mock(ServiceDiscoveryCallback.class);

        Map<String, NodeInfo> nodeInfoMap = new HashMap<>();
        nodeInfoMap.put(UUID.randomUUID().toString(), createTestNodeInfo("127.0.0.1:9200"));
        nodeInfoMap.put(UUID.randomUUID().toString(), createTestNodeInfo("127.0.0.1:9201"));
        nodeInfoMap.put(UUID.randomUUID().toString(), createTestNodeInfo("127.0.0.1:9202"));

        NodesResponse nodesResponse = new NodesResponse(nodeInfoMap);
        when(httpClient.execute(any(), any())).thenReturn(nodesResponse);

        // when
        query.execute(httpClient, callback);

        // then
        verify(callback, never()).onFailure(any());

        @SuppressWarnings("unchecked") ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(callback).onSuccess(captor.capture());

        assertEquals(3, captor.getValue().size());
        assertThat(captor.getValue(), hasItems(
                expectedScheme + "://127.0.0.1:9200",
                expectedScheme + "://127.0.0.1:9201",
                expectedScheme + "://127.0.0.1:9202"));

    }

    @Test
    public void notifiesCallbackWithEmptyResultIfResponseSucceededAndHadNoNodes() {

        // given
        String expectedScheme = UUID.randomUUID().toString();
        ElasticsearchNodesQuery query = createDefaultTestQuery(expectedScheme);

        HttpClient httpClient = mock(HttpClient.class);
        ServiceDiscoveryCallback<List<String>> callback = mock(ServiceDiscoveryCallback.class);

        NodesResponse nodesResponse = new NodesResponse(new HashMap<>());
        when(httpClient.execute(any(), any())).thenReturn(nodesResponse);

        // when
        query.execute(httpClient, callback);

        // then
        verify(callback, never()).onFailure(any());

        @SuppressWarnings("unchecked") ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(callback).onSuccess(captor.capture());

        assertEquals(0, captor.getValue().size());

    }

    @Test
    public void notifiesCallbackWithEmptyResultIfResponseNotSucceeded() {

        // given
        String expectedScheme = UUID.randomUUID().toString();
        ElasticsearchNodesQuery query = createDefaultTestQuery(expectedScheme);

        HttpClient httpClient = mock(HttpClient.class);
        ServiceDiscoveryCallback<List<String>> callback = mock(ServiceDiscoveryCallback.class);

        NodesResponse nodesResponse = new NodesResponse(null);
        when(httpClient.execute(any(), any())).thenReturn(nodesResponse);

        // when
        query.execute(httpClient, callback);

        // then
        verify(callback, never()).onFailure(any());
        verify(callback).onSuccess(eq(Collections.emptyList()));

    }

    @Test
    public void notifiesCallbackOnException() {

        // given
        String expectedScheme = UUID.randomUUID().toString();
        ElasticsearchNodesQuery query = createDefaultTestQuery(expectedScheme);

        HttpClient httpClient = mock(HttpClient.class);
        String expectedMessage = "test exception";
        when(httpClient.execute(any(), any())).thenThrow(new RuntimeException(expectedMessage));

        // when
        ServiceDiscoveryCallback<List<String>> callback = mock(ServiceDiscoveryCallback.class);
        query.execute(httpClient, callback);

        // then
        verify(callback, never()).onSuccess(any());

        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(RuntimeException.class);
        verify(callback).onFailure(captor.capture());

        assertEquals(expectedMessage, captor.getValue().getMessage());

    }

    @Test
    public void responseHandlerCanProvideErrorMessage() {

        // given
        String expectedScheme = UUID.randomUUID().toString();
        ElasticsearchNodesQuery query = createDefaultTestQuery(expectedScheme);

        HttpClient httpClient = mock(HttpClient.class);
        String expectedMessage = "test exception";
        when(httpClient.execute(any(), any())).thenThrow(new RuntimeException(expectedMessage));

        ServiceDiscoveryCallback<List<String>> callback = mock(ServiceDiscoveryCallback.class);
        query.execute(httpClient, callback);
        @SuppressWarnings("unchecked") ArgumentCaptor<BlockingResponseHandler<NodesResponse>> captor =
                ArgumentCaptor.forClass(BlockingResponseHandler.class);
        verify(httpClient).execute(any(), captor.capture());
        BlockingResponseHandler<NodesResponse> handler = captor.getValue();

        // when
        handler.failed(new RuntimeException("another exception"));

        // then
        assertThat(handler.getResult().getErrorMessage(), containsString("Unable to refresh server list"));
        assertThat(handler.getResult().getErrorMessage(), containsString("another exception"));

    }

    @Test
    public void appliesGivenNodesFilter() {

        // given
        String expectedFilter = UUID.randomUUID().toString();

        String expectedScheme = UUID.randomUUID().toString();
        ElasticsearchNodesQuery query = createDefaultTestQuery(expectedScheme, expectedFilter);

        HttpClient httpClient = mock(HttpClient.class);
        NodesResponse nodesResponse = new NodesResponse(null);
        when(httpClient.execute(any(), any())).thenReturn(nodesResponse);

        ServiceDiscoveryCallback<List<String>> callback = mock(ServiceDiscoveryCallback.class);

        // when
        query.execute(httpClient, callback);

        // then
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        verify(httpClient).execute(captor.capture(), any());
        Request request = captor.getValue();

        assertThat(request.getURI(), containsString(expectedFilter));

    }

    @Test
    public void appliesDefaultNodesFilterIfNotProvided() {

        // given
        String expectedScheme = UUID.randomUUID().toString();
        ElasticsearchNodesQuery query = new ElasticsearchNodesQuery(expectedScheme);

        HttpClient httpClient = mock(HttpClient.class);
        NodesResponse nodesResponse = new NodesResponse(null);
        when(httpClient.execute(any(), any())).thenReturn(nodesResponse);

        ServiceDiscoveryCallback<List<String>> callback = mock(ServiceDiscoveryCallback.class);

        // when
        query.execute(httpClient, callback);

        // then
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        verify(httpClient).execute(captor.capture(), any());
        Request request = captor.getValue();

        assertThat(request.getURI(), containsString(ElasticsearchNodesQuery.DEFAULT_NODES_FILTER));

    }

    private ElasticsearchNodesQuery createDefaultTestQuery(String expectedScheme) {
        return createDefaultTestQuery(expectedScheme, ElasticsearchNodesQuery.DEFAULT_NODES_FILTER);
    }

    private ElasticsearchNodesQuery createDefaultTestQuery(String expectedScheme, String nodesFilter) {
        return new ElasticsearchNodesQuery(expectedScheme, nodesFilter);
    }

    private NodeInfo createTestNodeInfo(String publishAddress) {

        NodeInfo.PublishAddress httpPublishAddress = new NodeInfo.PublishAddress();
        httpPublishAddress.setPublishAddress(publishAddress);

        NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.setHttpPublishAddress(httpPublishAddress);

        return nodeInfo;

    }

}
