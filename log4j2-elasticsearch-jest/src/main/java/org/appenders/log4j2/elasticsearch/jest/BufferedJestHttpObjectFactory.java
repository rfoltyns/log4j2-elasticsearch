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


import io.searchbox.action.AbstractDocumentTargetedAction;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.JestResultHandler;
import io.searchbox.core.Bulk;
import io.searchbox.core.DocumentResult;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.ClientProvider;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;
import org.appenders.log4j2.elasticsearch.ItemSourceFactory;
import org.appenders.log4j2.elasticsearch.JacksonMixIn;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.failover.FailedItemOps;
import org.appenders.log4j2.elasticsearch.jest.failover.BufferedHttpFailedItemOps;

import java.util.function.Function;

@Plugin(name = BufferedJestHttpObjectFactory.PLUGIN_NAME, category = Node.CATEGORY, elementType = ClientObjectFactory.ELEMENT_TYPE, printObject = true)
public class BufferedJestHttpObjectFactory extends JestHttpObjectFactory {

    public static final String PLUGIN_NAME = "JestBufferedHttp";

    private static Logger LOG = InternalLogging.getLogger();

    private volatile State state = State.STOPPED;

    private final PooledItemSourceFactory itemSourceFactory;

    private final JacksonMixIn[] mixIns;

    protected BufferedJestHttpObjectFactory(Builder builder) {
        super(builder);
        this.itemSourceFactory = builder.pooledItemSourceFactory;
        this.mixIns = builder.mixIns;
    }

    @Override
    public Function<Bulk, Boolean> createFailureHandler(FailoverPolicy failover) {
        return bulk -> {
            BufferedBulk bufferedBulk = (BufferedBulk)bulk;
            LOG.warn(String.format("Batch of %s items failed. Redirecting to %s", bufferedBulk.getActions().size(), failover.getClass().getName()));
            try {
                bufferedBulk.getActions().stream()
                        .map(item -> failedItemOps.createItem(((BufferedIndex) item)))
                        .forEach(failover::deliver);
                return true;
            } catch (Exception e) {
                LOG.error("Unable to execute failover", e);
                return false;
            }
        };
    }

    @Override
    public BatchOperations<Bulk> createBatchOperations() {
        return new BufferedBulkOperations(itemSourceFactory, mixIns, mappingType);
    }

    protected JestResultHandler<JestResult> createResultHandler(Bulk bulk, Function<Bulk, Boolean> failureHandler) {
        return new JestResultHandler<JestResult>() {

            @Override
            public void completed(JestResult result) {

                backoffPolicy.deregister(bulk);

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
                backoffPolicy.deregister(bulk);
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
    ClientProvider<JestClient> getClientProvider(WrappedHttpClientConfig.Builder clientConfigBuilder) {
        return new JestClientProvider(clientConfigBuilder) {
            @Override
            public JestClient createClient() {
                WrappedHttpClientConfig wrappedHttpClientConfig = clientConfigBuilder.build();
                JestClientFactory jestClientFactory = new BufferedJestClientFactory(wrappedHttpClientConfig);
                return jestClientFactory.getObject();
            }
        };
    }

    public static class Builder extends JestHttpObjectFactory.Builder {

        @PluginElement(ItemSourceFactory.ELEMENT_TYPE)
        protected PooledItemSourceFactory pooledItemSourceFactory;

        @PluginElement(JacksonMixIn.ELEMENT_TYPE)
        private JacksonMixIn[] mixIns = new JacksonMixIn[0];

        @Override
        public BufferedJestHttpObjectFactory build() {

            validate();

            return new BufferedJestHttpObjectFactory(this);
        }

        protected void validate() {

            super.validate();

            if (pooledItemSourceFactory == null) {
                throw new ConfigurationException("No PooledItemSourceFactory configured for BufferedJestHttpObjectFactory");
            }
        }

        protected FailedItemOps<AbstractDocumentTargetedAction<DocumentResult>> failedItemOps() {
            return new BufferedHttpFailedItemOps();
        }

        public Builder withItemSourceFactory(PooledItemSourceFactory pooledItemSourceFactory) {
            this.pooledItemSourceFactory = pooledItemSourceFactory;
            return this;
        }

        public Builder withMixIns(JacksonMixIn[] mixIns) {
            this.mixIns = mixIns;
            return this;
        }

    }

    // ==========
    // LIFECYCLE
    // ==========

    @Override
    public void start() {
        super.start();
        if (!itemSourceFactory.isStarted()) {
            itemSourceFactory.start();
        }
        state = State.STARTED;
    }

    @Override
    public void stop() {
        super.stop();
        if (!itemSourceFactory.isStopped()) {
            itemSourceFactory.stop();
        }
        state = State.STOPPED;
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
