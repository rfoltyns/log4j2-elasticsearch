package org.appenders.log4j2.elasticsearch;

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
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

public class TestHttpObjectFactory implements ClientObjectFactory<TestClient, BulkEmitterTest.TestBatch> {

    private final Collection<String> serverUris;
    private final int connTimeout;
    private final int readTimeout;
    private final int maxTotalConnections;
    private final int defaultMaxTotalConnectionsPerRoute;
    private final boolean discoveryEnabled;

    protected TestHttpObjectFactory(Collection<String> serverUris, int connTimeout, int readTimeout, int maxTotalConnections, int defaultMaxTotalConnectionPerRoute, boolean discoveryEnabled) {
        this.serverUris = serverUris;
        this.connTimeout = connTimeout;
        this.readTimeout = readTimeout;
        this.maxTotalConnections = maxTotalConnections;
        this.defaultMaxTotalConnectionsPerRoute = defaultMaxTotalConnectionPerRoute;
        this.discoveryEnabled = discoveryEnabled;
    }

    protected TestHttpObjectFactory(Builder builder) {
        this(Arrays.asList(builder.serverUris, builder.serverUris),
                builder.connTimeout,
                builder.readTimeout,
                builder.maxTotalConnection,
                builder.defaultMaxTotalConnectionPerRoute,
                builder.discoveryEnabled);
    }

    @Override
    public Collection<String> getServerList() {
        return new ArrayList<>(serverUris);
    }

    @Override
    public TestClient createClient() {
        return new TestClient();
    }

    @Override
    public Function<BulkEmitterTest.TestBatch, Boolean> createBatchListener(FailoverPolicy failoverPolicy) {
        return new Function<BulkEmitterTest.TestBatch, Boolean>() {

            private Function<BulkEmitterTest.TestBatch, Boolean> failureHandler = createFailureHandler(failoverPolicy);
            private TestClient client = createClient();

            @Override
            public Boolean apply(BulkEmitterTest.TestBatch bulk) {
                TestResultHandler<Object> jestResultHandler = createResultHandler(bulk, failureHandler);
                client.executeAsync(bulk, jestResultHandler);
                return true;
            }

        };
    }

    @Override
    public Function<BulkEmitterTest.TestBatch, Boolean> createFailureHandler(FailoverPolicy failover) {
        return bulk -> {
            bulk.items.forEach(failedItem -> failover.deliver(failedItem));
            return true;
        };
    }

    @Override
    public BatchOperations<BulkEmitterTest.TestBatch> createBatchOperations() {
        return new BulkEmitterTest.TestBatchOperations();
    }

    @Override
    public void execute(IndexTemplate indexTemplate) {
    }

    @Override
    public void addOperation(Operation operation) {
        try {
            operation.execute();
        } catch (Exception e) {
        }
    }

    @Override
    public OperationFactory setupOperationFactory() {
        return new OperationFactoryDispatcher() {
            {
                OperationFactory operationFactory = new OperationFactory() {
                    @Override
                    public <T extends OpSource> Operation create(T opSource) {
                        return () -> Collections.singletonList(new DummySetupStep());
                    }
                };
                register(IndexTemplate.TYPE_NAME, operationFactory);
                register(ILMPolicy.TYPE_NAME, operationFactory);
            }
        };
    }

    protected TestResultHandler<Object> createResultHandler(BulkEmitterTest.TestBatch bulk, Function<BulkEmitterTest.TestBatch, Boolean> failureHandler) {
        return new TestResultHandler<Object>() {
        };
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<TestHttpObjectFactory> {

        @PluginBuilderAttribute
        @Required(message = "No serverUris provided for JestClientConfig")
        private String serverUris;

        @PluginBuilderAttribute
        private int connTimeout = -1;

        @PluginBuilderAttribute
        private int readTimeout = -1;

        @PluginBuilderAttribute
        private int maxTotalConnection = 40;

        @PluginBuilderAttribute
        private int defaultMaxTotalConnectionPerRoute = 4;

        @PluginBuilderAttribute
        private boolean discoveryEnabled;

        @Override
        public TestHttpObjectFactory build() {
            if (serverUris == null) {
                throw new ConfigurationException("No serverUris provided for JestClientConfig");
            }
            return new TestHttpObjectFactory(Arrays.asList(serverUris.split(";")), connTimeout, readTimeout, maxTotalConnection, defaultMaxTotalConnectionPerRoute, discoveryEnabled);
        }

        public Builder withServerUris(String serverUris) {
            this.serverUris = serverUris;
            return this;
        }

        public void withMaxTotalConnection(int maxTotalConnection) {
            this.maxTotalConnection = maxTotalConnection;
        }

        public void withDefaultMaxTotalConnectionPerRoute(int defaultMaxTotalConnectionPerRoute) {
            this.defaultMaxTotalConnectionPerRoute = defaultMaxTotalConnectionPerRoute;
        }

        public void withConnTimeout(int connTimeout) {
            this.connTimeout = connTimeout;
        }

        public void withReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
        }

        public void withDiscoveryEnabled(boolean discoveryEnabled) {
            this.discoveryEnabled = discoveryEnabled;
        }

    }

}
