package org.appenders.log4j2.elasticsearch.hc;

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
import org.appenders.log4j2.elasticsearch.NoopFailoverPolicy;
import org.junit.Test;
import org.mockito.Mockito;

import static org.appenders.log4j2.elasticsearch.hc.HCHttpTest.createDefaultHttpObjectFactoryBuilder;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;

public class BulkEmitterFactoryTest {


    @Test
    public void acceptsClientObjectFactory() {

        // given
        BatchEmitterFactory emitterFactory = new BulkEmitterFactory();

        // when
        boolean result = emitterFactory.accepts(HCHttp.class);

        // then
        assertTrue(result);
    }

    @Test
    public void acceptsExtendingClientObjectFactories() {

        // given
        BatchEmitterFactory emitterFactory = new BulkEmitterFactory();

        // when
        boolean result = emitterFactory.accepts(TestHCHttp.class);

        // then
        assertTrue(result);
    }

    @Test
    public void createsBatchListener() {

        // given
        BatchEmitterFactory factory = new BulkEmitterFactory();
        HCHttp clientObjectFactory = Mockito.spy(createDefaultHttpObjectFactoryBuilder().build());
        NoopFailoverPolicy failoverPolicy = new NoopFailoverPolicy();

        // when
        factory.createInstance(1, 1, clientObjectFactory, failoverPolicy);

        // then
        Mockito.verify(clientObjectFactory).createBatchListener(eq(failoverPolicy));

    }

    @Test
    public void createsBatchOperations() {

        // given
        BatchEmitterFactory factory = new BulkEmitterFactory();
        HCHttp clientObjectFactory = Mockito.spy(createDefaultHttpObjectFactoryBuilder().build());

        // when
        factory.createInstance(1, 1, clientObjectFactory, new NoopFailoverPolicy());

        // then
        Mockito.verify(clientObjectFactory).createBatchOperations();

    }

    public static class TestHCHttp extends HCHttp {
        protected TestHCHttp() {
            super(createDefaultHttpObjectFactoryBuilder());
        }
    }

}
