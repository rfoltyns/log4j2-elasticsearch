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
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.ClientProvider;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.Operation;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkRequestIntrospector;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import static org.appenders.core.logging.InternalLogging.getLogger;

@Plugin(name = BulkProcessorObjectFactory.PLUGIN_NAME, category = Node.CATEGORY, elementType = ClientObjectFactory.ELEMENT_TYPE, printObject = true)
public class BulkProcessorObjectFactory implements ClientObjectFactory<TransportClient, BulkRequest> {

    static final String PLUGIN_NAME = "ElasticsearchBulkProcessor";

    private final Collection<String> serverUris;
    private final UriParser uriParser = new UriParser();
    private final Auth auth;
    private final ClientSettings clientSettings;

    private TransportClient client;

    protected BulkProcessorObjectFactory(Collection<String> serverUris, Auth auth) {
        this(serverUris, auth, new ClientSettings.Builder().build());
    }

    protected BulkProcessorObjectFactory(Collection<String> serverUris, Auth auth, ClientSettings clientSettings) {
        this.serverUris = serverUris;
        this.auth = auth;
        this.clientSettings = clientSettings;
    }

    @Override
    public Collection<String> getServerList() {
        return new ArrayList<>(serverUris);
    }

    @Override
    public TransportClient createClient() {
        if (client == null) {
            TransportClient client = getClientProvider().createClient();
            for (String serverUri : serverUris) {
                try {
                    String host = uriParser.getHost(serverUri);
                    int port = uriParser.getPort(serverUri);
                    client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), port));
                } catch (UnknownHostException e) {
                    throw new ConfigurationException(e.getMessage());
                }
            }
            this.client = client;
        }
        return client;
    }

    // visible for testing
    ClientProvider<TransportClient> getClientProvider() {
        return auth == null ? new InsecureTransportClientProvider(clientSettings) : new SecureClientProvider(auth, clientSettings);
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

    @Override
    public void execute(IndexTemplate indexTemplate) {
        try {
            createClient().admin().indices().putTemplate(
                    new PutIndexTemplateRequest()
                            .name(indexTemplate.getName())
                            .source(indexTemplate.getSource())
            );
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }

    @Override
    public void addOperation(Operation operation) {
        try {
            operation.execute();
        } catch (Exception e) {
            getLogger().error("Operation failed: {}", e.getMessage());
        }
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<BulkProcessorObjectFactory> {

        public static final ClientSettings DEFAULT_CLIENT_SETTINGS = ClientSettings.newBuilder().build();

        @PluginBuilderAttribute
        @Required(message = "No serverUris provided for " + PLUGIN_NAME)
        private String serverUris;

        @PluginElement("auth")
        private Auth auth;

        @PluginElement(ClientSettings.ELEMENT_TYPE)
        private ClientSettings clientSettings = DEFAULT_CLIENT_SETTINGS;

        @Override
        public BulkProcessorObjectFactory build() {
            if (serverUris == null) {
                throw new ConfigurationException("No serverUris provided for " + PLUGIN_NAME);
            }
            return new BulkProcessorObjectFactory(Arrays.asList(serverUris.split(";")), auth, clientSettings);
        }

        public Builder withServerUris(String serverUris) {
            this.serverUris = serverUris;
            return this;
        }

        public Builder withAuth(Auth auth) {
            this.auth = auth;
            return this;
        }

        public Builder withClientSettings(ClientSettings clientSettings) {
            this.clientSettings = clientSettings;
            return this;
        }

    }

    static class InsecureTransportClientProvider implements ClientProvider<TransportClient> {

        private final ClientSettings clientSettings;

        InsecureTransportClientProvider() {
            this.clientSettings = Builder.DEFAULT_CLIENT_SETTINGS;
        }

        InsecureTransportClientProvider(ClientSettings clientSettings) {
            this.clientSettings = clientSettings;
        }

        @Override
        public TransportClient createClient() {

            Settings.Builder settingsBuilder = Settings.builder();
            this.clientSettings.applyTo(settingsBuilder);

            return TransportClient
                    .builder()
                    .settings(settingsBuilder.build())
                    .build();
        }

    }

}
