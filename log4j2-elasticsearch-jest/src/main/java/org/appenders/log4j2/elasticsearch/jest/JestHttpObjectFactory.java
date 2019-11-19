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


import io.searchbox.action.AbstractDocumentTargetedAction;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.client.JestResultHandler;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Bulk;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Index;
import io.searchbox.core.JestBatchIntrospector;
import io.searchbox.indices.template.PutTemplate;
import io.searchbox.indices.template.TemplateAction;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.status.StatusLogger;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.ClientProvider;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.Operation;
import org.appenders.log4j2.elasticsearch.failover.FailedItemOps;
import org.appenders.log4j2.elasticsearch.jest.failover.JestHttpFailedItemOps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

import static org.appenders.log4j2.elasticsearch.jest.JestBulkOperations.DEFAULT_MAPPING_TYPE;

@Plugin(name = "JestHttp", category = Node.CATEGORY, elementType = ClientObjectFactory.ELEMENT_TYPE, printObject = true)
public class JestHttpObjectFactory implements ClientObjectFactory<JestClient, Bulk> {

    private static Logger LOG = StatusLogger.getLogger();

    private volatile State state = State.STOPPED;

    private final Collection<String> serverUris;
    private final int connTimeout;
    private final int readTimeout;
    private final int maxTotalConnections;
    private final int defaultMaxTotalConnectionsPerRoute;
    private final int ioThreadCount;
    private final boolean discoveryEnabled;
    private final Auth<io.searchbox.client.config.HttpClientConfig.Builder> auth;
    protected final String mappingType;

    private final ConcurrentLinkedQueue<Operation> operations = new ConcurrentLinkedQueue<>();

    protected FailedItemOps<AbstractDocumentTargetedAction<DocumentResult>> failedItemOps;

    private JestClient client;

    /**
     * This constructor is deprecated and will be removed in 1.5.
     * Use {@link #JestHttpObjectFactory(Collection, int, int, int, int, int, boolean, Auth, String)} instead.
     *
     * @param serverUris List of semicolon-separated `http[s]://host:[port]` addresses of Elasticsearch nodes to connect with. Unless `discoveryEnabled=true`, this will be the final list of available nodes
     * @param connTimeout Number of milliseconds before ConnectException is thrown while attempting to connect
     * @param readTimeout Number of milliseconds before SocketTimeoutException is thrown while waiting for response bytes
     * @param maxTotalConnections Number of connections available
     * @param defaultMaxTotalConnectionPerRoute Number of connections available per Apache CPool
     * @param discoveryEnabled If `true`, `io.searchbox.client.config.discovery.NodeChecker` will use `serverUris` to auto-discover Elasticsearch nodes. Otherwise, `serverUris` will be the final list of available nodes
     * @param auth Security configuration
     * @deprecated As of 1.5, this constructor wil be removed. Use {@link #JestHttpObjectFactory(Builder)} instead.
     *
     */
    @Deprecated
    protected JestHttpObjectFactory(Collection<String> serverUris,
                                    int connTimeout,
                                    int readTimeout,
                                    int maxTotalConnections,
                                    int defaultMaxTotalConnectionPerRoute,
                                    boolean discoveryEnabled,
                                    Auth<io.searchbox.client.config.HttpClientConfig.Builder> auth) {
        this(serverUris,
                connTimeout,
                readTimeout,
                maxTotalConnections,
                defaultMaxTotalConnectionPerRoute,
                Runtime.getRuntime().availableProcessors(),
                discoveryEnabled,
                auth,
                DEFAULT_MAPPING_TYPE);
    }

    /**
     * @param serverUris List of semicolon-separated `http[s]://host:[port]` addresses of Elasticsearch nodes to connect with. Unless `discoveryEnabled=true`, this will be the final list of available nodes
     * @param connTimeout Number of milliseconds before ConnectException is thrown while attempting to connect
     * @param readTimeout Number of milliseconds before SocketTimeoutException is thrown while waiting for response bytes
     * @param maxTotalConnections Number of connections available
     * @param defaultMaxTotalConnectionPerRoute Number of connections available per Apache CPool
     * @param discoveryEnabled If `true`, `io.searchbox.client.config.discovery.NodeChecker` will use `serverUris` to auto-discover Elasticsearch nodes. Otherwise, `serverUris` will be the final list of available nodes
     * @param ioThreadCount number of 'I/O Dispatcher' threads started by Apache HC `IOReactor`
     * @param auth Security configuration
     * @param mappingType Elasticsearch mapping type name. MAY be set to '_doc' for Elasticsearch 7.x compatibility
     * @deprecated As of 1.5, this constructor will be removed. Use {@link #JestHttpObjectFactory(Builder)} instead
     */
    @Deprecated
    protected JestHttpObjectFactory(Collection<String> serverUris,
                                    int connTimeout,
                                    int readTimeout,
                                    int maxTotalConnections,
                                    int defaultMaxTotalConnectionPerRoute,
                                    int ioThreadCount,
                                    boolean discoveryEnabled,
                                    Auth<io.searchbox.client.config.HttpClientConfig.Builder> auth,
                                    String mappingType) {
        this.serverUris = serverUris;
        this.connTimeout = connTimeout;
        this.readTimeout = readTimeout;
        this.maxTotalConnections = maxTotalConnections;
        this.defaultMaxTotalConnectionsPerRoute = defaultMaxTotalConnectionPerRoute;
        this.ioThreadCount = ioThreadCount;
        this.discoveryEnabled = discoveryEnabled;
        this.auth = auth;
        this.mappingType = mappingType;
    }

    protected JestHttpObjectFactory(Builder builder) {
        this.serverUris = Arrays.asList(builder.serverUris.split(";"));
        this.connTimeout = builder.connTimeout;
        this.readTimeout = builder.readTimeout;
        this.maxTotalConnections = builder.maxTotalConnection;
        this.defaultMaxTotalConnectionsPerRoute = builder.defaultMaxTotalConnectionPerRoute;
        this.ioThreadCount = builder.ioThreadCount;
        this.discoveryEnabled = builder.discoveryEnabled;
        this.auth = builder.auth;
        this.mappingType = builder.mappingType;
        this.failedItemOps = builder.failedItemOps;
    }

    @Override
    public Collection<String> getServerList() {
        return new ArrayList<>(serverUris);
    }

    @Override
    public JestClient createClient() {
        if (client == null) {

            HttpClientConfig.Builder builder = new HttpClientConfig.Builder(serverUris)
                    .maxTotalConnection(maxTotalConnections)
                    .defaultMaxTotalConnectionPerRoute(defaultMaxTotalConnectionsPerRoute)
                    .connTimeout(connTimeout)
                    .readTimeout(readTimeout)
                    .discoveryEnabled(discoveryEnabled)
                    .multiThreaded(true);

            if (this.auth != null) {
                auth.configure(builder);
            }

            WrappedHttpClientConfig.Builder wrappedHttpClientConfigBuilder =
                    new WrappedHttpClientConfig.Builder(builder.build())
                            .ioThreadCount(ioThreadCount);

            client = getClientProvider(wrappedHttpClientConfigBuilder).createClient();
        }
        return client;
    }

    @Override
    public Function<Bulk, Boolean> createBatchListener(FailoverPolicy failoverPolicy) {
        return new Function<Bulk, Boolean>() {

            private Function<Bulk, Boolean> failureHandler = createFailureHandler(failoverPolicy);

            @Override
            public Boolean apply(Bulk bulk) {

                while (!operations.isEmpty()) {
                    try {
                        operations.remove().execute();
                    } catch (Exception e) {
                        // TODO: redirect to failover (?) retry with exp. backoff (?) multiple options here
                        LOG.error("Deferred operation failed: {}", e.getMessage());
                    }
                }

                JestResultHandler<JestResult> jestResultHandler = createResultHandler(bulk, failureHandler);
                createClient().executeAsync(bulk, jestResultHandler);
                return true;
            }

        };
    }

    @Override
    public Function<Bulk, Boolean> createFailureHandler(FailoverPolicy failover) {
        return new Function<Bulk, Boolean>() {

            private final JestBatchIntrospector introspector = new JestBatchIntrospector();

            @Override
            public Boolean apply(Bulk bulk) {

                Collection items = introspector.items(bulk);

                LOG.warn(String.format("Batch of %s items failed. Redirecting to %s",
                        items.size(),
                        failover.getClass().getName()));

                items.forEach(item -> {
                    Index failedAction = (Index) item;
                    failover.deliver(failedItemOps.createItem(failedAction));
                });

                return true;
            }

        };
    }

    @Override
    public BatchOperations<Bulk> createBatchOperations() {
        return new JestBulkOperations(mappingType);
    }

    @Override
    public void execute(IndexTemplate indexTemplate) {
        TemplateAction templateAction = new PutTemplate.Builder(indexTemplate.getName(), indexTemplate.getSource()).build();
        try {
            JestResult result = createClient().execute(templateAction);
            if (!result.isSucceeded()) {
                LOG.error("IndexTemplate not added: " + result.getErrorMessage());
            }
        } catch (IOException e) {
            LOG.error("IndexTemplate not added: " + e.getMessage());
        }
    }

    @Override
    public void addOperation(Operation operation) {
        operations.add(operation);
    }

    protected JestResultHandler<JestResult> createResultHandler(Bulk bulk, Function<Bulk, Boolean> failureHandler) {
        return new JestResultHandler<JestResult>() {
            @Override
            public void completed(JestResult result) {
                if (!result.isSucceeded()) {
                    LOG.warn(result.getErrorMessage());
                    failureHandler.apply(bulk);
                }
            }
            @Override
            public void failed(Exception ex) {
                LOG.warn(ex.getMessage(), ex);
                failureHandler.apply(bulk);
            }
        };
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    // visible for testing
    ClientProvider<JestClient> getClientProvider(WrappedHttpClientConfig.Builder clientConfigBuilder) {
        return new JestClientProvider(clientConfigBuilder);
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<JestHttpObjectFactory> {

        @PluginBuilderAttribute
        @Required(message = "No serverUris provided for JestClientConfig")
        protected String serverUris;

        @PluginBuilderAttribute
        protected int connTimeout = -1;

        @PluginBuilderAttribute
        protected int readTimeout = -1;

        @PluginBuilderAttribute
        protected int maxTotalConnection = 40;

        @PluginBuilderAttribute
        protected int defaultMaxTotalConnectionPerRoute = 4;

        @PluginBuilderAttribute
        protected int ioThreadCount = Runtime.getRuntime().availableProcessors();

        @PluginBuilderAttribute
        protected boolean discoveryEnabled;

        @PluginElement("auth")
        protected Auth auth;

        /**
         * Since 1.3.5, index mapping type can be specified to ensure compatibility with ES 7 clusters
         *
         * By default, "index" until 1.4, then "_doc"
         */
        @PluginBuilderAttribute
        protected String mappingType = DEFAULT_MAPPING_TYPE;

        protected FailedItemOps<AbstractDocumentTargetedAction<DocumentResult>> failedItemOps = failedItemOps();

        @Override
        public JestHttpObjectFactory build() {

            validate();

            return new JestHttpObjectFactory(this);
        }

        protected FailedItemOps<AbstractDocumentTargetedAction<DocumentResult>> failedItemOps() {
            return new JestHttpFailedItemOps();
        }

        protected void validate() {
            if (serverUris == null) {
                throw new ConfigurationException("No serverUris provided for " + JestHttpObjectFactory.class.getName());
            }
        }

        public Builder withServerUris(String serverUris) {
            this.serverUris = serverUris;
            return this;
        }

        public Builder withMaxTotalConnection(int maxTotalConnection) {
            this.maxTotalConnection = maxTotalConnection;
            return this;
        }

        public Builder withDefaultMaxTotalConnectionPerRoute(int defaultMaxTotalConnectionPerRoute) {
            this.defaultMaxTotalConnectionPerRoute = defaultMaxTotalConnectionPerRoute;
            return this;
        }

        public Builder withConnTimeout(int connTimeout) {
            this.connTimeout = connTimeout;
            return this;
        }

        public Builder withReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder withIoThreadCount(int ioThreadCount) {
            this.ioThreadCount = ioThreadCount;
            return this;
        }

        public Builder withDiscoveryEnabled(boolean discoveryEnabled) {
            this.discoveryEnabled = discoveryEnabled;
            return this;
        }

        public Builder withAuth(Auth auth) {
            this.auth = auth;
            return this;
        }

        public Builder withMappingType(String mappingType) {
            this.mappingType = mappingType;
            return this;
        }

    }

    /**
     * Consider this class <i>private</i>.
     */
    class JestClientProvider implements ClientProvider<JestClient> {

        private final WrappedHttpClientConfig.Builder clientConfigBuilder;

        public JestClientProvider(WrappedHttpClientConfig.Builder clientConfigBuilder) {
            this.clientConfigBuilder = clientConfigBuilder;
        }

        @Override
        public JestClient createClient() {
            ExtendedJestClientFactory jestClientFactory = new ExtendedJestClientFactory(clientConfigBuilder.build());
            return jestClientFactory.getObject();
        }

    }

    @Override
    public void start() {
        // DON'T START THE CLIENT HERE! client started in scope of JestHttpClient
        state = State.STARTED;
    }

    @Override
    public void stop() {

        if (isStopped()) {
            return;
        }

        LOG.debug("Stopping {}", getClass().getSimpleName());

        if (client != null) {
            client.shutdownClient();
        }
        state = State.STOPPED;

        LOG.debug("{} stopped", getClass().getSimpleName());

    }

    @Override
    public boolean isStarted() {
        return state == State.STARTED;
    }

    @Override
    public boolean isStopped() {
        return state == State.STOPPED;
    }
}
