package org.appenders.log4j2.elasticsearch.ahc;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2022 Rafal Foltynski
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

import java.util.Random;

import static org.appenders.log4j2.elasticsearch.ahc.AHCHttpTest.createDefaultHttpObjectFactoryBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class BulkEmitterFactoryTest {


    @Test
    public void acceptsClientObjectFactory() {

        // given
        final BatchEmitterFactory emitterFactory = new BulkEmitterFactory();

        // when
        final boolean result = emitterFactory.accepts(AHCHttp.class);

        // then
        assertTrue(result);
    }

    @Test
    public void acceptsExtendingClientObjectFactories() {

        // given
        final BatchEmitterFactory emitterFactory = new BulkEmitterFactory();

        // when
        final boolean result = emitterFactory.accepts(TestAHCHttp.class);

        // then
        assertTrue(result);
    }

    @Test
    public void createsBatchListener() {

        // given
        final BatchEmitterFactory factory = new BulkEmitterFactory();
        final AHCHttp clientObjectFactory = spy(createDefaultHttpObjectFactoryBuilder().build());
        final NoopFailoverPolicy failoverPolicy = new NoopFailoverPolicy();

        // when
        factory.createInstance(1, 1, clientObjectFactory, failoverPolicy);

        // then
        verify(clientObjectFactory).createBatchListener(eq(failoverPolicy));

    }

    @Test
    public void createsBatchOperations() {

        // given
        final BatchEmitterFactory factory = new BulkEmitterFactory();
        final AHCHttp clientObjectFactory = spy(createDefaultHttpObjectFactoryBuilder().build());

        // when
        factory.createInstance(1, 1, clientObjectFactory, new NoopFailoverPolicy());

        // then
        verify(clientObjectFactory).createBatchOperations();

    }

    @Test
    public void loadingOrderCanBeOverriddenWithProperty() {

        // given
        final BulkEmitterFactory factory = new BulkEmitterFactory();

        final int expectedLoadingOrder = new Random().nextInt(100) + 1;
        System.setProperty("appenders." + BulkEmitterFactory.class.getSimpleName() + ".loadingOrder", Integer.toString(expectedLoadingOrder));

        // when
        final int loadingOrder = factory.loadingOrder();

        // then
        assertEquals(expectedLoadingOrder, loadingOrder);

    }

    @Test
    public void defaultLoadingOrderIsReturnedIfPropertyNotSet() {

        // given
        final int expectedLoadingOrder = BatchEmitterFactory.DEFAULT_LOADING_ORDER + 10;

        final BulkEmitterFactory factory = new BulkEmitterFactory();

        System.clearProperty("appenders." + BulkEmitterFactory.class.getSimpleName() + ".loadingOrder");

        // when
        final int loadingOrder = factory.loadingOrder();

        // then
        assertEquals(expectedLoadingOrder, loadingOrder);

    }

    public static class TestAHCHttp extends AHCHttp {
        protected TestAHCHttp() {
            super(createDefaultHttpObjectFactoryBuilder());
        }
    }

}
