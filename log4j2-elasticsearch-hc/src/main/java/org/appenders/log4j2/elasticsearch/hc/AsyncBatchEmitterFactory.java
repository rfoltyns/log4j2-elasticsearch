package org.appenders.log4j2.elasticsearch.hc;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2021 Rafal Foltynski
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

import org.appenders.log4j2.elasticsearch.AsyncBatchEmitter;
import org.appenders.log4j2.elasticsearch.BatchEmitterFactory;
import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;

public class AsyncBatchEmitterFactory implements BatchEmitterFactory<AsyncBatchEmitter> {

    @Override
    public boolean accepts(Class clientObjectFactoryClass) {
        return HCHttp.class.isAssignableFrom(clientObjectFactoryClass);
    }

    @Override
    public int loadingOrder() {
        String priority = System.getProperty("appenders." + AsyncBatchEmitterFactory.class.getSimpleName() + ".loadingOrder");
        if (priority == null) {
            return DEFAULT_LOADING_ORDER + 9;
        }
        return Integer.parseInt(priority);
    }

    @Override
    public AsyncBatchEmitter createInstance(int batchSize, int deliveryInterval, ClientObjectFactory clientObjectFactory, FailoverPolicy failoverPolicy) {
        AsyncBatchEmitter bulkEmitter = new AsyncBatchEmitter(batchSize, deliveryInterval, clientObjectFactory.createBatchOperations());
        bulkEmitter.addListener(clientObjectFactory.createBatchListener(failoverPolicy));
        return bulkEmitter;
    }

}
