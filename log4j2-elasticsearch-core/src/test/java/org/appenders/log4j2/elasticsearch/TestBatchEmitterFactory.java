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



import org.appenders.log4j2.elasticsearch.spi.BatchEmitterServiceProvider;
import org.mockito.Mockito;

public class TestBatchEmitterFactory extends BatchEmitterServiceProvider implements BatchEmitterFactory<BatchEmitter> {

    private BatchEmitter spiedEmitter;

    @Override
    public boolean accepts(Class clientObjectFactoryClass) {
        return TestHttpObjectFactory.class.isAssignableFrom(clientObjectFactoryClass);
    }

    @Override
    public BatchEmitter createInstance(int batchSize, int deliveryInterval, ClientObjectFactory clientObjectFactory, FailoverPolicy failoverPolicy) {
        if (spiedEmitter == null) {
            BulkEmitter emitter = new BulkEmitter(batchSize, deliveryInterval, clientObjectFactory.createBatchOperations());
            emitter.addListener(clientObjectFactory.createBatchListener(failoverPolicy));
            spiedEmitter = Mockito.spy(emitter);
        }
        return spiedEmitter;
    }
}
