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

import org.appenders.log4j2.elasticsearch.ahc.BlockingResponseHandler;
import org.appenders.log4j2.elasticsearch.ahc.ElasticsearchBulkAPI;
import org.appenders.log4j2.elasticsearch.ahc.GenericRequest;
import org.appenders.log4j2.elasticsearch.ahc.HttpClient;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link ServiceDiscovery} integration for Elasticsearch.
 */
public class ElasticsearchNodesQuery implements ServiceDiscoveryRequest<HttpClient> {

    public static final String DEFAULT_NODES_FILTER = "_all";

    private final BlockingResponseHandler<NodesResponse> responseHandler = new BlockingResponseHandler<>(
            ElasticsearchBulkAPI.defaultObjectMapper().readerFor(NodesResponse.class),
            (ex) -> new NodesResponse(Collections.emptyMap()).withErrorMessage("Unable to refresh server list: " + ex.getMessage())
    );

    protected final String resultScheme;
    protected final String nodesFilter;

    public ElasticsearchNodesQuery(final String resultScheme) {
        this(resultScheme, DEFAULT_NODES_FILTER);
    }

    public ElasticsearchNodesQuery(final String resultScheme, final String nodesFilter) {
        this.resultScheme = resultScheme;
        this.nodesFilter = nodesFilter;
    }

    /**
     * Executes request using given client.
     *
     * @param httpClient client to use
     * @param callback address list consumer
     */
    @Override
    public void execute(final HttpClient httpClient, final ServiceDiscoveryCallback<List<String>> callback) {

        final GenericRequest request = new GenericRequest("GET", String.format("_nodes/%s/http", nodesFilter), null);
        final NodesResponse response;

        try {
            response = httpClient.execute(request, responseHandler);
        } catch (Exception e) {
            callback.onFailure(e);
            return;
        }

        if (response.isSucceeded()) {
            callback.onSuccess(response.getNodes()
                    .values()
                    .stream()
                    .map(this::formatAddress)
                    .collect(Collectors.toList()));
        } else {
            callback.onSuccess(Collections.emptyList());
        }

    }

    protected String formatAddress(final NodeInfo info) {
        return String.format("%s://%s", resultScheme, info.getHttpPublishAddress().getPublishAddress());
    }

}
