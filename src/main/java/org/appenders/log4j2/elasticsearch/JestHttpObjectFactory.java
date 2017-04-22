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
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.JestResultHandler;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Bulk;
import io.searchbox.core.JestBatchIntrospector;

@Plugin(name = "JestHttp", category = Node.CATEGORY, elementType = ClientObjectFactory.ELEMENT_TYPE, printObject = true)
public class JestHttpObjectFactory implements ClientObjectFactory<JestClient, Bulk> {

    private final Collection<String> serverUris;
    private final int connTimeout;
    private final int readTimeout;
    private final int maxTotalConnections;
    private final int defaultMaxTotalConnectionsPerRoute;
    private final boolean discoveryEnabled;

    protected JestHttpObjectFactory(Collection<String> serverUris, int connTimeout, int readTimeout, int maxTotalConnections, int defaultMaxTotalConnectionPerRoute, boolean discoveryEnabled) {
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
    public JestClient createClient() {
        JestClientFactory factory = new JestClientFactory();

        HttpClientConfig.Builder builder = new HttpClientConfig.Builder(serverUris);
        builder.maxTotalConnection(maxTotalConnections);
        builder.defaultMaxTotalConnectionPerRoute(defaultMaxTotalConnectionsPerRoute);
        builder.connTimeout(connTimeout);
        builder.readTimeout(readTimeout);
        builder.discoveryEnabled(discoveryEnabled);
        builder.multiThreaded(true);

        factory.setHttpClientConfig(builder.build());

        return factory.getObject();
    }

    @Override
    public Observer createBatchListener(FailoverPolicy failoverPolicy) {
        return new Observer() {

            private Function<Bulk, Boolean> failureHandler = createFailureHandler(failoverPolicy);
            private JestClient client = createClient();

            @Override
            public void update(Observable o, Object bulk) {
                JestBatchIntrospector introspector = new JestBatchIntrospector();
                JestResultHandler<JestResult> jestResultHandler = createResultHandler((Bulk) bulk, failureHandler);
                client.executeAsync((Bulk) bulk, jestResultHandler);
            }

        };
    }

    @Override
    public Function<Bulk, Boolean> createFailureHandler(FailoverPolicy failover) {
        return new Function<Bulk, Boolean>() {

            private final JestBatchIntrospector introspector = new JestBatchIntrospector();

            @Override
            public Boolean apply(Bulk bulk) {
                introspector.items(bulk).forEach(failedItem -> failover.deliver(failedItem));
                return true;
            }

        };
    }

    protected JestResultHandler<JestResult> createResultHandler(Bulk bulk, Function<Bulk, Boolean> failureHandler) {
        return new JestResultHandler<JestResult>() {
            @Override
            public void completed(JestResult result) {
                if (!result.isSucceeded()) {
                    failureHandler.apply(bulk);
                }
            }
            @Override
            public void failed(Exception ex) {
                failureHandler.apply(bulk);
            }
        };
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<JestHttpObjectFactory> {

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
        public JestHttpObjectFactory build() {
            if (serverUris == null) {
                throw new ConfigurationException("No serverUris provided for JestClientConfig");
            }
            return new JestHttpObjectFactory(Arrays.asList(serverUris.split(";")), connTimeout, readTimeout, maxTotalConnection, defaultMaxTotalConnectionPerRoute, discoveryEnabled);
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
