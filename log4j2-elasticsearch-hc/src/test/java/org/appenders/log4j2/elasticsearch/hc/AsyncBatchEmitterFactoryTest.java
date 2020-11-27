package org.appenders.log4j2.elasticsearch.hc;

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