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


import io.searchbox.action.AbstractAction;
import io.searchbox.action.AbstractDocumentTargetedAction;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.client.JestResultHandler;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Bulk;
import io.searchbox.core.BulkResult;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Index;
import io.searchbox.core.JestBatchIntrospector;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.ClientProvider;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.Log4j2Lookup;
import org.appenders.log4j2.elasticsearch.Operation;
import org.appenders.log4j2.elasticsearch.OperationFactory;
import org.appenders.log4j2.elasticsearch.Result;
import org.appenders.log4j2.elasticsearch.SetupStep;
import org.appenders.log4j2.elasticsearch.ValueResolver;
import org.appenders.log4j2.elasticsearch.backoff.BackoffPolicy;
import org.appenders.log4j2.elasticsearch.backoff.NoopBackoffPolicy;
import org.appenders.log4j2.elasticsearch.failover.FailedItemOps;
import org.appenders.log4j2.elasticsearch.jest.failover.JestHttpFailedItemOps;
import org.appenders.log4j2.elasticsearch.util.SplitUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

import static org.appenders.core.logging.InternalLogging.getLogger;
import static org.appenders.log4j2.elasticsearch.jest.JestBulkOperations.DEFAULT_MAPPING_TYPE;

@Plugin(name = "JestHttp", category = Node.CATEGORY, elementType = ClientObjectFactory.ELEMENT_TYPE, printObject = true)
public class JestHttpObjectFactory implements ClientObjectFactory<JestClient, Bulk> {

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
    protected final FailedItemOps<AbstractDocumentTargetedAction<DocumentResult>> failedItemOps;
    protected final BackoffPolicy<AbstractAction<BulkResult>> backoffPolicy;

    private final ConcurrentLinkedQueue<Operation> operations = new ConcurrentLinkedQueue<>();
    private final ValueResolver valueResolver;
    private OperationFactory setupOps;
    private JestClient client;

    protected JestHttpObjectFactory(Builder builder) {
        this.serverUris = SplitUtil.split(builder.serverUris, ";");
        this.connTimeout = builder.connTimeout;
        this.readTimeout = builder.readTimeout;
        this.maxTotalConnections = builder.maxTotalConnection;
        this.defaultMaxTotalConnectionsPerRoute = builder.defaultMaxTotalConnectionPerRoute;
        this.ioThreadCount = builder.ioThreadCount;
        this.discoveryEnabled = builder.discoveryEnabled;
        this.auth = builder.auth;
        this.mappingType = builder.mappingType;
        this.failedItemOps = builder.failedItemOps;
        this.backoffPolicy = builder.backoffPolicy;
        this.valueResolver = builder.valueResolver;
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

            private final Function<Bulk, Boolean> failureHandler = createFailureHandler(failoverPolicy);

            @Override
            public Boolean apply(Bulk bulk) {

                executePreBatchOperations();

                if (backoffPolicy.shouldApply(bulk)) {
                    getLogger().warn("Backoff applied. Request rejected.");
                    failureHandler.apply(bulk);
                    return false;
                } else {
                    backoffPolicy.register(bulk);
                }

                JestResultHandler<JestResult> jestResultHandler = createResultHandler(bulk, failureHandler);
                createClient().executeAsync(bulk, jestResultHandler);
                return true;
            }

        };
    }

    /* visible for testing */
    int executePreBatchOperations() {
        int executionCount = 0;
        while (!operations.isEmpty()) {
            try {
                operations.remove().execute();
            } catch (Exception e) {
                // TODO: redirect to failover (?) retry with exp. backoff (?) multiple options here
                getLogger().error("Deferred operation failed: {}", e.getMessage());
            } finally {
                executionCount++;
            }
        }
        return executionCount;
    }

    @Override
    public Function<Bulk, Boolean> createFailureHandler(FailoverPolicy failover) {
        return new Function<Bulk, Boolean>() {

            private final JestBatchIntrospector introspector = new JestBatchIntrospector();

            @Override
            public Boolean apply(Bulk bulk) {

                Collection items = introspector.items(bulk);

                getLogger().warn(String.format("Batch of %s items failed. Redirecting to %s",
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
        try {
            setupOperationFactory().create(indexTemplate).execute();
        } catch (Exception e) {
            getLogger().error("IndexTemplate not added", e);
        }
    }

    private Result executeOperation(SetupStep<GenericJestRequest, JestResult> operation) {
        try {
            JestResult result = createClient().execute(operation.createRequest());
            return operation.onResponse(result);
        } catch (IOException e) {
            return operation.onException(e);
        }
    }

    @Override
    public void addOperation(Operation operation) {
        operations.add(operation);
    }

    @Override
    public OperationFactory setupOperationFactory() {
        // FIXME: move to constructor
        if (setupOps == null) {
            setupOps = new JestOperationFactoryDispatcher(this::executeOperation, valueResolver);
        }
        return setupOps;
    }

    protected JestResultHandler<JestResult> createResultHandler(Bulk bulk, Function<Bulk, Boolean> failureHandler) {
        return new JestResultHandler<JestResult>() {
            @Override
            public void completed(JestResult result) {

                backoffPolicy.deregister(bulk);

                if (!result.isSucceeded()) {
                    getLogger().warn(result.getErrorMessage());
                    failureHandler.apply(bulk);
                }
            }
            @Override
            public void failed(Exception ex) {
                getLogger().warn(ex.getMessage(), ex);
                backoffPolicy.deregister(bulk);
                failureHandler.apply(bulk);
            }
        };
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    /* visible for testing */
    ValueResolver valueResolver() {
        return valueResolver;
    }

    // visible for testing
    ClientProvider<JestClient> getClientProvider(WrappedHttpClientConfig.Builder clientConfigBuilder) {
        return new JestClientProvider(clientConfigBuilder);
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<JestHttpObjectFactory> {

        private static final BackoffPolicy<AbstractAction<BulkResult>> DEFAULT_BACKOFF_POLICY =
                new NoopBackoffPolicy<>();

        // TODO move to JestHttpPlugin
        @PluginConfiguration
        private Configuration configuration;

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
         * Index mapping type can be specified to ensure compatibility with ES 7 clusters
         * <br>
         * {@code _doc} by default
         *
         * Since 1.3.5
         */
        @PluginBuilderAttribute
        protected String mappingType = DEFAULT_MAPPING_TYPE;

        @PluginElement("backoffPolicy")
        protected BackoffPolicy<AbstractAction<BulkResult>> backoffPolicy = DEFAULT_BACKOFF_POLICY;

        protected FailedItemOps<AbstractDocumentTargetedAction<DocumentResult>> failedItemOps = failedItemOps();

        protected ValueResolver valueResolver;

        @Override
        public JestHttpObjectFactory build() {

            validate();

            resolveLazyProperties();

            return new JestHttpObjectFactory(this);
        }

        protected void resolveLazyProperties() {
            this.valueResolver = getValueResolver();
        }

        private ValueResolver getValueResolver() {

            // allow programmatic override
            if (valueResolver != null) {
                return valueResolver;
            }

            // handle XML config
            if (configuration != null) {
                return new Log4j2Lookup(configuration.getStrSubstitutor());
            }

            // fallback to no-op
            return ValueResolver.NO_OP;
        }

        protected FailedItemOps<AbstractDocumentTargetedAction<DocumentResult>> failedItemOps() {
            return new JestHttpFailedItemOps();
        }

        protected void validate() {
            if (serverUris == null) {
                throw new ConfigurationException("No serverUris provided for " + JestHttpObjectFactory.class.getSimpleName());
            }
            if (backoffPolicy == null) {
                throw new ConfigurationException("No BackoffPolicy provided for JestHttp");
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

        public Builder withBackoffPolicy(BackoffPolicy<AbstractAction<BulkResult>> backoffPolicy) {
            this.backoffPolicy = backoffPolicy;
            return this;
        }

        public Builder withConfiguration(Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder withValueResolver(ValueResolver valueResolver) {
            this.valueResolver = valueResolver;
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

        getLogger().debug("Stopping {}", getClass().getSimpleName());

        if (client != null) {
            client.shutdownClient();
        }
        state = State.STOPPED;

        getLogger().debug("{} stopped", getClass().getSimpleName());

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
