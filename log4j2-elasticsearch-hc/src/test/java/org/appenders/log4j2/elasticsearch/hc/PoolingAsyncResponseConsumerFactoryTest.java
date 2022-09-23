package org.appenders.log4j2.elasticsearch.hc;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2019 Rafal Foltynski
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

import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
import org.appenders.log4j2.elasticsearch.GenericItemSourcePool;
import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PoolingAsyncResponseConsumerFactoryTest {

    @Test
    public void producesPoolingAsyncResponseConsumer() {

        // given
        PoolingAsyncResponseConsumerFactory factory = createAsyncResponseConsumerFactory();

        // when
        HttpAsyncResponseConsumer result = factory.create();

        // then
        assertTrue(result instanceof PoolingAsyncResponseConsumer);

    }

    @Test
    public void lifecycleStartStartPoolOnlyOnce() {

        // given
        GenericItemSourcePool pool = mock(GenericItemSourcePool.class);
        PoolingAsyncResponseConsumerFactory objectFactory = createAsyncResponseConsumerFactory(pool);

        // when
        objectFactory.start();
        objectFactory.start();

        // then
        verify(pool).start();

    }

    @Test
    public void lifecycleStopStopsPoolOnlyOnce() {

        // given
        GenericItemSourcePool pool = mock(GenericItemSourcePool.class);
        PoolingAsyncResponseConsumerFactory objectFactory = createAsyncResponseConsumerFactory(pool);

        objectFactory.start();

        objectFactory.create();

        // when
        objectFactory.stop();
        objectFactory.stop();

        // then
        verify(pool).stop();

    }

    @Test
    public void lifecycleStart() {

        // given
        LifeCycle lifeCycle = createLifeCycleTestObject();

        assertTrue(lifeCycle.isStopped());

        // when
        lifeCycle.start();

        // then
        assertFalse(lifeCycle.isStopped());
        assertTrue(lifeCycle.isStarted());

    }

    @Test
    public void lifecycleStop() {

        // given
        LifeCycle lifeCycle = createLifeCycleTestObject();

        assertTrue(lifeCycle.isStopped());

        lifeCycle.start();
        assertTrue(lifeCycle.isStarted());

        // when
        lifeCycle.stop();

        // then
        assertFalse(lifeCycle.isStarted());
        assertTrue(lifeCycle.isStopped());

    }

    private PoolingAsyncResponseConsumerFactory createAsyncResponseConsumerFactory() {
        return createAsyncResponseConsumerFactory(mock(GenericItemSourcePool.class));
    }

    private PoolingAsyncResponseConsumerFactory createAsyncResponseConsumerFactory(GenericItemSourcePool pool) {
        return new PoolingAsyncResponseConsumerFactory(pool);
    }

    private LifeCycle createLifeCycleTestObject() {
        return createAsyncResponseConsumerFactory();
    }

}
