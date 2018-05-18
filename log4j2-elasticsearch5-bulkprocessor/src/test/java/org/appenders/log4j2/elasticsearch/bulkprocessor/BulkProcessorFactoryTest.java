package org.appenders.log4j2.elasticsearch.bulkprocessor;

/*-
 * #%L
 * log4j-elasticsearch
 * %%
 * Copyright (C) 2018 Rafal Foltynski
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */


import org.appenders.log4j2.elasticsearch.Auth;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Matchers;

import java.util.Collection;
import java.util.function.Function;

import static org.mockito.Matchers.any;
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
    public void acceptsClientObjectFactoriesExtendingBulkProcessorFactory() {

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
        verify(failureHandler, times(1)).apply(Matchers.eq(request));

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

    public static class ExtendedBulkProcessorObjectFactory extends BulkProcessorObjectFactory {
        protected ExtendedBulkProcessorObjectFactory(Collection<String> serverUris, Auth auth) {
            super(serverUris, auth);
        }
    }
}
