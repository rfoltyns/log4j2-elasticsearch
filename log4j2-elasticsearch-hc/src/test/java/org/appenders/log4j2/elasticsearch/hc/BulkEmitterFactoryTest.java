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
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Random;

import static org.appenders.log4j2.elasticsearch.hc.HCHttpTest.createDefaultHttpObjectFactoryBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;

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

    @Test
    public void loadingOrderCanBeOverriddenWithProperty() {

        // given
        BulkEmitterFactory factory = new BulkEmitterFactory();

        int expectedLoadingOrder = new Random().nextInt(100) + 1;
        System.setProperty("appenders." + BulkEmitterFactory.class.getSimpleName() + ".loadingOrder", Integer.toString(expectedLoadingOrder));

        // when
        int loadingOrder = factory.loadingOrder();

        // then
        assertEquals(expectedLoadingOrder, loadingOrder);

    }

    @Test
    public void defaultLoadingOrderIsReturnedIfPropertyNotSet() {

        // given
        int expectedLoadingOrder = BatchEmitterFactory.DEFAULT_LOADING_ORDER + 10;

        BulkEmitterFactory factory = new BulkEmitterFactory();

        System.clearProperty("appenders." + BulkEmitterFactory.class.getSimpleName() + ".loadingOrder");

        // when
        int loadingOrder = factory.loadingOrder();

        // then
        assertEquals(expectedLoadingOrder, loadingOrder);

    }

    public static class TestHCHttp extends HCHttp {
        protected TestHCHttp() {
            super(createDefaultHttpObjectFactoryBuilder());
        }
    }

}
