package org.appenders.log4j2.elasticsearch.jest;

/*-
 * #%L
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



import java.io.IOException;
import java.util.*;
import java.util.function.Function;

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

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.JestResultHandler;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Bulk;
import io.searchbox.core.JestBatchIntrospector;
import org.apache.logging.log4j.status.StatusLogger;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.ClientProvider;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;
import org.appenders.log4j2.elasticsearch.IndexTemplate;

@Plugin(name = "JestHttp", category = Node.CATEGORY, elementType = ClientObjectFactory.ELEMENT_TYPE, printObject = true)
public class JestHttpObjectFactory implements ClientObjectFactory<JestClient, Bulk> {

    private static Logger LOG = StatusLogger.getLogger();

    private final Collection<String> serverUris;
    private final int connTimeout;
    private final int readTimeout;
    private final int maxTotalConnections;
    private final int defaultMaxTotalConnectionsPerRoute;
    private final boolean discoveryEnabled;
    private final Auth<HttpClientConfig.Builder> auth;

    private JestClient client;

    protected JestHttpObjectFactory(Collection<String> serverUris, int connTimeout, int readTimeout, int maxTotalConnections, int defaultMaxTotalConnectionPerRoute, boolean discoveryEnabled, Auth<HttpClientConfig.Builder> auth) {
        this.serverUris = serverUris;
        this.connTimeout = connTimeout;
        this.readTimeout = readTimeout;
        this.maxTotalConnections = maxTotalConnections;
        this.defaultMaxTotalConnectionsPerRoute = defaultMaxTotalConnectionPerRoute;
        this.discoveryEnabled = discoveryEnabled;
        this.auth = auth;
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

            client = getClientProvider(builder).createClient();
        }
        return client;
    }

    @Override
    public Function<Bulk, Boolean> createBatchListener(FailoverPolicy failoverPolicy) {
        return new Function<Bulk, Boolean>() {

            private Function<Bulk, Boolean> failureHandler = createFailureHandler(failoverPolicy);
            private JestClient client = createClient();

            @Override
            public Boolean apply(Bulk bulk) {
                JestResultHandler<JestResult> jestResultHandler = createResultHandler(bulk, failureHandler);
                client.executeAsync(bulk, jestResultHandler);
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
                introspector.items(bulk).forEach(failedItem -> failover.deliver(failedItem));
                return true;
            }

        };
    }

    @Override
    public BatchOperations<Bulk> createBatchOperations() {
        return new JestBulkOperations();
    }

    @Override
    public void execute(IndexTemplate indexTemplate) {
        TemplateAction templateAction = new PutTemplate.Builder(indexTemplate.getName(), indexTemplate.getSource()).build();
        try {
            JestResult result = createClient().execute(templateAction);
            if (!result.isSucceeded()) {
                throw new ConfigurationException("IndexTemplate not added: " + result.getErrorMessage());
            }
        } catch (IOException e) {
            throw new ConfigurationException("IndexTemplate not added: " + e.getMessage());
        }
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

    // visible for testing
    ClientProvider<JestClient> getClientProvider(HttpClientConfig.Builder clientConfigBuilder) {
        return new JestClientProvider(clientConfigBuilder);
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

        @PluginElement("auth")
        private Auth auth;

        @Override
        public JestHttpObjectFactory build() {
            if (serverUris == null) {
                throw new ConfigurationException("No serverUris provided for JestClientConfig");
            }
            return new JestHttpObjectFactory(Arrays.asList(serverUris.split(";")), connTimeout, readTimeout, maxTotalConnection, defaultMaxTotalConnectionPerRoute, discoveryEnabled, auth);
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

        public Builder withDiscoveryEnabled(boolean discoveryEnabled) {
            this.discoveryEnabled = discoveryEnabled;
            return this;
        }

        public Builder withAuth(Auth auth) {
            this.auth = auth;
            return this;
        }
    }

    class JestClientProvider implements ClientProvider<JestClient> {

        private final HttpClientConfig.Builder clientConfigBuilder;

        public JestClientProvider(HttpClientConfig.Builder clientConfigBuilder) {
            this.clientConfigBuilder = clientConfigBuilder;
        }

        @Override
        public JestClient createClient() {
            JestClientFactory jestClientFactory = new JestClientFactory();
            jestClientFactory.setHttpClientConfig(clientConfigBuilder.build());
            return jestClientFactory.getObject();
        }

    }
}
