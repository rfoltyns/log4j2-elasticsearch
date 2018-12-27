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


import org.appenders.log4j2.elasticsearch.BatchEmitter;
import org.appenders.log4j2.elasticsearch.BatchEmitterFactory;
import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;

import java.util.function.Function;

public class BulkProcessorFactory implements BatchEmitterFactory {

    @Override
    public boolean accepts(Class clientObjectFactoryClass) {
        return BulkProcessorObjectFactory.class.isAssignableFrom(clientObjectFactoryClass);
    }

    @Override
    public BatchEmitter createInstance(int batchSize, int deliveryInterval, ClientObjectFactory clientObjectFactory, FailoverPolicy failoverPolicy) {

        Function<BulkRequest, Boolean> failureHandler = clientObjectFactory.createFailureHandler(failoverPolicy);

        BulkProcessor.Listener listener = new BulkExecutionListener(failureHandler);

        BulkProcessor.Builder builder = BulkProcessor.builder((Client) clientObjectFactory.createClient(), listener)
                .setBulkActions(batchSize)
                .setFlushInterval(TimeValue.timeValueMillis(deliveryInterval));
        return new BulkProcessorDelegate(builder.build());
    }

    class BulkExecutionListener implements BulkProcessor.Listener {

        private Function<BulkRequest, Boolean> failureHandler;

        BulkExecutionListener(Function<BulkRequest, Boolean> failureHandler) {
            this.failureHandler = failureHandler;
        }

        @Override
        public void beforeBulk(long executionId, BulkRequest request) {
        }

        @Override
        public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
            if (response.hasFailures()) {
                failureHandler.apply(request);
            }

        }

        @Override
        public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
            failureHandler.apply(request);
        }
    }
}
