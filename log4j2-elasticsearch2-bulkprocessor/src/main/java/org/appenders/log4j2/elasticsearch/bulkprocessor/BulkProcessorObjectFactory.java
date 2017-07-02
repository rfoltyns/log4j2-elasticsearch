package org.appenders.log4j2.elasticsearch.bulkprocessor;

/*-
 * #%L
 * log4j2-elasticsearch
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


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.function.Function;

import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkRequestIntrospector;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

@Plugin(name = "ElasticsearchBulkProcessor", category = Node.CATEGORY, elementType = ClientObjectFactory.ELEMENT_TYPE, printObject = true)
public class BulkProcessorObjectFactory implements ClientObjectFactory<TransportClient, BulkRequest> {

    private final Collection<String> serverUris;
    private final UriParser uriParser = new UriParser();

    protected BulkProcessorObjectFactory(Collection<String> serverUris) {
        this.serverUris = serverUris;
    }

    @Override
    public Collection<String> getServerList() {
        return new ArrayList<>(serverUris);
    }

    @Override
    public TransportClient createClient() {

        TransportClient client = TransportClient
                .builder()
                .settings(Settings.builder().EMPTY_SETTINGS)
                .build();

        for (String serverUri : serverUris) {
            try {
                String host = uriParser.getHost(serverUri);
                int port = uriParser.getPort(serverUri);
                client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), port));
            } catch (UnknownHostException e) {
                throw new ConfigurationException(e.getMessage());
            }
        }

        return client;
    }

    @Override
    public Function<BulkRequest, Boolean> createBatchListener(FailoverPolicy failoverPolicy) {
        return noop -> true;
    }

    @Override
    public Function<BulkRequest, Boolean> createFailureHandler(FailoverPolicy failover) {
        return new Function<BulkRequest, Boolean>() {

            private final BulkRequestIntrospector introspector = new BulkRequestIntrospector();

            @Override
            public Boolean apply(BulkRequest bulk) {
                introspector.items(bulk).forEach(failedItem -> failover.deliver(failedItem));
                return true;
            }

        };
    }

    @Override
    public BatchOperations<BulkRequest> createBatchOperations() {
        return new ElasticsearchBatchOperations();
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<BulkProcessorObjectFactory> {

        @PluginBuilderAttribute
        @Required(message = "No serverUris provided for JestClientConfig")
        private String serverUris;

        @Override
        public BulkProcessorObjectFactory build() {
            if (serverUris == null) {
                throw new ConfigurationException("No serverUris provided for JestClientConfig");
            }
            return new BulkProcessorObjectFactory(Arrays.asList(serverUris.split(";")));
        }

        public Builder withServerUris(String serverUris) {
            this.serverUris = serverUris;
            return this;
        }

    }

}
