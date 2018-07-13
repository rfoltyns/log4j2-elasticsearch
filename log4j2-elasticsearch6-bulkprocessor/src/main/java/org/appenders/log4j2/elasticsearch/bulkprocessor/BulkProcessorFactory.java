package org.appenders.log4j2.elasticsearch.bulkprocessor;

/*-
 * #%L
 * log4j-elasticsearch
 * %%
 * Copyright (C) 2017 Rafal Foltynski
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


import org.appenders.log4j2.elasticsearch.*;
import org.appenders.log4j2.elasticsearch.BatchEmitterFactory;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;

import java.util.function.Function;

public class BulkProcessorFactory implements BatchEmitterFactory {

    @Override
    public boolean accepts(Class clientObjectFactoryClass) {
        return BulkProcessorObjectFactory.class.isAssignableFrom(clientObjectFactoryClass);
    }

    @Override
    public BatchEmitter createInstance(int batchSize, int deliveryInterval, ClientObjectFactory clientObjectFactory, FailoverPolicy failoverPolicy) {

        Function<BulkRequest, Boolean> failureHandler = clientObjectFactory.createFailureHandler(failoverPolicy);

        BulkProcessor.Listener listener = new BulkExecutionListener(failureHandler);

        BulkProcessor.Builder builder = BulkProcessor.builder((Client) clientObjectFactory.createClient(), listener)
                .setBulkActions(batchSize)
                .setFlushInterval(TimeValue.timeValueMillis(deliveryInterval));
        return new BulkProcessorDelegate(builder.build());
    }

    class BulkExecutionListener implements BulkProcessor.Listener {

        private Function<BulkRequest, Boolean> failureHandler;

        BulkExecutionListener(Function<BulkRequest, Boolean> failureHandler) {
            this.failureHandler = failureHandler;
        }

        @Override
        public void beforeBulk(long executionId, BulkRequest request) {
        }

        @Override
        public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
            if (response.hasFailures()) {
                failureHandler.apply(request);
            }

        }

        @Override
        public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
            failureHandler.apply(request);
        }
    }
}
