package org.appenders.log4j2.elasticsearch.jest;

/*-
 * #%L
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


import io.netty.buffer.ByteBuf;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.JestResultHandler;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Bulk;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.status.StatusLogger;
import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.ClientProvider;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;
import org.appenders.log4j2.elasticsearch.ItemSourceFactory;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

@Plugin(name = BufferedJestHttpObjectFactory.PLUGIN_NAME, category = Node.CATEGORY, elementType = ClientObjectFactory.ELEMENT_TYPE, printObject = true)
public class BufferedJestHttpObjectFactory extends JestHttpObjectFactory {

    public static final String PLUGIN_NAME = "JestBufferedHttp";

    private static Logger LOG = StatusLogger.getLogger();

    private final PooledItemSourceFactory itemSourceFactoryConfig;

    protected BufferedJestHttpObjectFactory(
            Collection<String> serverUris,
            int connTimeout,
            int readTimeout,
            int maxTotalConnections,
            int defaultMaxTotalConnectionPerRoute,
            boolean discoveryEnabled,
            PooledItemSourceFactory bufferedSourceFactory,
            Auth<HttpClientConfig.Builder> auth
    ) {
        super(
                serverUris,
                connTimeout,
                readTimeout,
                maxTotalConnections,
                defaultMaxTotalConnectionPerRoute,
                discoveryEnabled,
                auth
        );
        this.itemSourceFactoryConfig = bufferedSourceFactory;
    }

    @Override
    public Function<Bulk, Boolean> createFailureHandler(FailoverPolicy failover) {
        return bulk -> {
            BufferedBulk bufferedBulk = (BufferedBulk)bulk;
            LOG.warn(String.format("Batch of %s items failed. Redirecting to %s", bufferedBulk.getActions().size(), failover.getClass().getName()));
            bufferedBulk.getActions().forEach(failedItem -> {
                ByteBuf byteBuf = ((BufferedIndex) failedItem).source.getSource();
                failover.deliver(byteBuf.toString(0, byteBuf.writerIndex(), Charset.defaultCharset()));
            });
            return true;
        };
    }

    @Override
    public BatchOperations<Bulk> createBatchOperations() {
        return new BufferedBulkOperations(itemSourceFactoryConfig);
    }

    protected JestResultHandler<JestResult> createResultHandler(Bulk bulk, Function<Bulk, Boolean> failureHandler) {
        return new JestResultHandler<JestResult>() {

            @Override
            public void completed(JestResult result) {
                if (!result.isSucceeded()) {
                    LOG.warn(result.getErrorMessage());
                    // TODO: filter only failed items when retry is ready.
                    // failing whole bulk for now
                    failureHandler.apply(bulk);
                }
                ((BufferedBulk)bulk).completed();
            }

            @Override
            public void failed(Exception ex) {
                LOG.warn(ex.getMessage(), ex);
                failureHandler.apply(bulk);
                ((BufferedBulk)bulk).completed();
            }

        };
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    // visible for testing
    ClientProvider<JestClient> getClientProvider(HttpClientConfig.Builder clientConfigBuilder) {
        return new JestClientProvider(clientConfigBuilder) {
            @Override
            public JestClient createClient() {
                JestClientFactory jestClientFactory = new BufferedJestClientFactory(clientConfigBuilder);
                jestClientFactory.setHttpClientConfig(clientConfigBuilder.build());
                return jestClientFactory.getObject();
            }
        };
    }

    public static class Builder extends JestHttpObjectFactory.Builder {

        @PluginElement(ItemSourceFactory.ELEMENT_TYPE)
        private PooledItemSourceFactory pooledItemSourceFactory;

        @Override
        public BufferedJestHttpObjectFactory build() {

            validate();

            return new BufferedJestHttpObjectFactory(
                    Arrays.asList(serverUris.split(";")),
                    connTimeout,
                    readTimeout,
                    maxTotalConnection,
                    defaultMaxTotalConnectionPerRoute,
                    discoveryEnabled,
                    pooledItemSourceFactory,
                    auth);
        }

        protected void validate() {

            super.validate();

            if (pooledItemSourceFactory == null) {
                throw new ConfigurationException("No PooledItemSourceFactory configured for BufferedJestHttpObjectFactory");
            }
        }

        public Builder withItemSourceFactory(PooledItemSourceFactory pooledItemSourceFactory) {
            this.pooledItemSourceFactory = pooledItemSourceFactory;
            return this;
        }
    }

}
