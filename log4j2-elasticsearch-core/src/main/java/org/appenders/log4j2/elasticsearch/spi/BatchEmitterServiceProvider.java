package org.appenders.log4j2.elasticsearch.spi;

/*-
 * #%L
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


import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.status.StatusLogger;
import org.appenders.log4j2.elasticsearch.BatchEmitter;
import org.appenders.log4j2.elasticsearch.BatchEmitterFactory;
import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;

import java.util.Iterator;
import java.util.ServiceLoader;

public class BatchEmitterServiceProvider {

    private static final Logger LOG = StatusLogger.getLogger();

    public BatchEmitter createInstance(int batchSize,
                                       int deliveryInterval,
                                       ClientObjectFactory clientObjectFactory,
                                       FailoverPolicy failoverPolicy) {

        ServiceLoader<BatchEmitterFactory> loader = ServiceLoader.load(BatchEmitterFactory.class);
        Iterator<BatchEmitterFactory> it = loader.iterator();
        while (it.hasNext()) {
            BatchEmitterFactory factory = it.next();
            LOG.info("BatchEmitterFactory class found {}", factory);
            if (factory.accepts(clientObjectFactory.getClass())) {
                LOG.info("Using {} as BatchEmitterFactoryProvider", factory);
                return factory.createInstance(batchSize, deliveryInterval, clientObjectFactory, failoverPolicy);
            }
        }

        throw new ConfigurationException(String.format("No compatible BatchEmitter implementations for %s found", clientObjectFactory.getClass()));

    }
}
