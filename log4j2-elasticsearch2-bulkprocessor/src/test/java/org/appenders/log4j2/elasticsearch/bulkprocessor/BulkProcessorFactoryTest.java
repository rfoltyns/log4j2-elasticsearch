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


import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.BatchEmitterFactory;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.Random;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BulkProcessorFactoryTest {

    @Test
    public void acceptsBulkProcessorObjectFactory() {

        // given
        BulkProcessorFactory emitterFactory = new BulkProcessorFactory();

        // when
        boolean result = emitterFactory.accepts(BulkProcessorObjectFactory.class);

        // then
        Assert.assertTrue(result);
    }

    @Test
    public void acceptsExtendingClientObjectFactories() {
    
        // given
        BulkProcessorFactory emitterFactory = new BulkProcessorFactory();

        // when
        boolean result = emitterFactory.accepts(ExtendedBulkProcessorObjectFactory.class);

        // then
        Assert.assertTrue(result);
    }

    @Test
    public void bulkExecutionListenerExecutesFailoverHandlerWhenResponseHasFailures() {

        // given
        BulkProcessorFactory emitterFactory = new BulkProcessorFactory();

        Function<BulkRequest, Boolean> failureHandler = mock(Function.class);
        BulkProcessorFactory.BulkExecutionListener listener =
                emitterFactory.new BulkExecutionListener(failureHandler);

        BulkRequest request = mock(BulkRequest.class);
        BulkResponse response = mock(BulkResponse.class);

        when(response.hasFailures()).thenReturn(true);

        // when
        listener.afterBulk(0, request, response);

        // then
        verify(failureHandler, times(1)).apply(eq(request));

    }

    @Test
    public void bulkExecutionListenerDoesntExecuteFailoverHandlerWhenResponseDoesntHaveFailures() {

        // given
        BulkProcessorFactory emitterFactory = new BulkProcessorFactory();

        Function<BulkRequest, Boolean> failureHandler = mock(Function.class);
        BulkProcessorFactory.BulkExecutionListener listener =
                emitterFactory.new BulkExecutionListener(failureHandler);

        BulkRequest request = mock(BulkRequest.class);
        BulkResponse response = mock(BulkResponse.class);

        when(response.hasFailures()).thenReturn(false);

        // when
        listener.afterBulk(0, request, response);

        // then
        verify(failureHandler, times(0)).apply(any());

    }

    @Test
    public void loadingOrderCanBeOverriddenWithProperty() {

        // given
        BulkProcessorFactory factory = new BulkProcessorFactory();

        int expectedLoadingOrder = new Random().nextInt(100) + 1;
        System.setProperty("appenders." + BulkProcessorFactory.class.getSimpleName() + ".loadingOrder", Integer.toString(expectedLoadingOrder));

        // when
        int loadingOrder = factory.loadingOrder();

        // then
        assertEquals(expectedLoadingOrder, loadingOrder);

    }

    @Test
    public void defaultLoadingOrderIsReturnedIfPropertyNotSet() {

        // given
        int expectedLoadingOrder = BatchEmitterFactory.DEFAULT_LOADING_ORDER + 10;

        BulkProcessorFactory factory = new BulkProcessorFactory();

        System.clearProperty("appenders." + BulkProcessorFactory.class.getSimpleName() + ".loadingOrder");

        // when
        int loadingOrder = factory.loadingOrder();

        // then
        assertEquals(expectedLoadingOrder, loadingOrder);

    }

    public static class ExtendedBulkProcessorObjectFactory extends BulkProcessorObjectFactory {
        protected ExtendedBulkProcessorObjectFactory(Collection<String> serverUris, Auth auth) {
            super(serverUris, auth);
        }
    }
}
