package org.appenders.log4j2.elasticsearch.hc;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2020 Rafal Foltynski
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
import org.appenders.log4j2.elasticsearch.NoopFailoverPolicy;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.appenders.log4j2.elasticsearch.hc.HCHttpTest.createDefaultHttpObjectFactoryBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class AsyncBatchEmitterFactoryTest {

    @Test
    public void acceptsClientObjectFactory() {

        // given
        BatchEmitterFactory emitterFactory = new AsyncBatchEmitterFactory();

        // when
        boolean result = emitterFactory.accepts(HCHttp.class);

        // then
        assertTrue(result);

    }

    @Test
    public void acceptsExtendingClientObjectFactories() {

        // given
        BatchEmitterFactory emitterFactory = new AsyncBatchEmitterFactory();

        // when
        boolean result = emitterFactory.accepts(AsyncBatchEmitterFactoryTest.TestHCHttp.class);

        // then
        assertTrue(result);

    }

    @Test
    public void createsBatchEmitter() {

        // given
        BatchEmitterFactory factory = new AsyncBatchEmitterFactory();
        HCHttp clientObjectFactory = spy(createDefaultHttpObjectFactoryBuilder().build());
        NoopFailoverPolicy failoverPolicy = new NoopFailoverPolicy();

        // when
        final BatchEmitter emitter = factory.createInstance(1, 1, clientObjectFactory, failoverPolicy);

        // then
        assertNotNull(emitter);
        verify(clientObjectFactory).createBatchListener(eq(failoverPolicy));
        verify(clientObjectFactory).createBatchOperations();

    }

    @Test
    public void loadingOrderCanBeOverriddenWithProperty() {

        // given
        AsyncBatchEmitterFactory factory = new AsyncBatchEmitterFactory();

        int expectedLoadingOrder = new Random().nextInt(100) + 1;
        System.setProperty("appenders." + AsyncBatchEmitterFactory.class.getSimpleName() + ".loadingOrder", Integer.toString(expectedLoadingOrder));

        // when
        int loadingOrder = factory.loadingOrder();

        // then
        assertEquals(expectedLoadingOrder, loadingOrder);

    }

    @Test
    public void defaultLoadingOrderIsReturnedIfOverrideNotSet() {

        // given
        int expectedLoadingOrder = BatchEmitterFactory.DEFAULT_LOADING_ORDER + 9;

        AsyncBatchEmitterFactory factory = new AsyncBatchEmitterFactory();

        System.clearProperty("appenders." + AsyncBatchEmitterFactory.class.getSimpleName() + ".loadingOrder");

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
