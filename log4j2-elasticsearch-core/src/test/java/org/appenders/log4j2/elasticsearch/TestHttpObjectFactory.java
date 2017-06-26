package org.appenders.log4j2.elasticsearch;

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


import java.util.*;
import java.util.function.Function;

import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

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
