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


import org.appenders.log4j2.elasticsearch.BatchEmitterFactory;
import org.appenders.log4j2.elasticsearch.BulkEmitter;
import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;

public class BulkEmitterFactory implements BatchEmitterFactory<BulkEmitter> {

    @Override
    public boolean accepts(Class clientObjectFactoryClass) {
        return JestHttpObjectFactory.class.isAssignableFrom(clientObjectFactoryClass);
    }

    /**
     * @return By default, {@link BatchEmitterFactory#DEFAULT_LOADING_ORDER} + 10. Can be overridden with {@code -Dappenders.BulkEmitterFactory.loadingOrder}
     */
    @Override
    public int loadingOrder() {
        String priority = System.getProperty("appenders." + BulkEmitterFactory.class.getSimpleName() + ".loadingOrder");
        if (priority == null) {
            return DEFAULT_LOADING_ORDER + 10;
        }
        return Integer.parseInt(priority);
    }

    @Override
    public BulkEmitter createInstance(int batchSize, int deliveryInterval, ClientObjectFactory clientObjectFactory, FailoverPolicy failoverPolicy) {
        BulkEmitter bulkEmitter = new BulkEmitter(batchSize, deliveryInterval, clientObjectFactory.createBatchOperations());
        bulkEmitter.addListener(clientObjectFactory.createBatchListener(failoverPolicy));
        return bulkEmitter;
    }

}
