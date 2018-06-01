package org.appenders.log4j2.elasticsearch.jest;

/*-
 * #%L
 * log4j2-elasticsearch
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


import org.appenders.log4j2.elasticsearch.BatchEmitterFactory;
import org.appenders.log4j2.elasticsearch.NoopFailoverPolicy;
import org.junit.Test;
import org.mockito.Mockito;
import static org.appenders.log4j2.elasticsearch.jest.JestHttpObjectFactoryTest.createTestObjectFactoryBuilder;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;

public class BulkEmitterFactoryTest {


    @Test
    public void acceptsBulkProcessorObjectFactory() {

        // given
        BatchEmitterFactory emitterFactory = new BulkEmitterFactory();

        // when
        boolean result = emitterFactory.accepts(JestHttpObjectFactory.class);

        // then
        assertTrue(result);
    }

    @Test
    public void acceptsExtendingClientObjectFactories() {

        // given
        BatchEmitterFactory emitterFactory = new BulkEmitterFactory();

        // when
        boolean result = emitterFactory.accepts(ExtendedBulkProcessorObjectFactory.class);

        // then
        assertTrue(result);
    }

    @Test
    public void createsBatchListener() {

        // given
        BatchEmitterFactory factory = new BulkEmitterFactory();
        JestHttpObjectFactory clientObjectFactory = spy(createTestObjectFactoryBuilder().build());
        NoopFailoverPolicy failoverPolicy = new NoopFailoverPolicy();

        // when
        factory.createInstance(1, 1, clientObjectFactory, failoverPolicy);

        // then
        Mockito.verify(clientObjectFactory).createBatchListener(eq(failoverPolicy));

    }

    @Test
    public void createsBulkOperations() {

        // given
        BatchEmitterFactory factory = new BulkEmitterFactory();
        JestHttpObjectFactory clientObjectFactory = spy(createTestObjectFactoryBuilder().build());

        // when
        factory.createInstance(1, 1, clientObjectFactory, new NoopFailoverPolicy());

        // then
        Mockito.verify(clientObjectFactory).createBatchOperations();

    }

    public static class ExtendedBulkProcessorObjectFactory extends JestHttpObjectFactory {
        protected ExtendedBulkProcessorObjectFactory() {
            super(null, 0, 0, 0, 0, false, null);
        }
    }
}
