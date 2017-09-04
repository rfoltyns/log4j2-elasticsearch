package org.appenders.log4j2.elasticsearch;

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

/**
 * SPI for all {@link BatchEmitter} factories.
 * <p>
 * Instances of implementing classes are created by {@link org.appenders.log4j2.elasticsearch.spi.BatchEmitterServiceProvider}
 * using {@link java.util.ServiceLoader}.
 * <p>
 * Given that multiple factories might be available in runtime, {@link BatchEmitterFactory#accepts accepts()} can
 * validate compatibility with {@link ClientObjectFactory}.
 *
 * @param <T> return type
 */
public interface BatchEmitterFactory<T extends BatchEmitter> {

    /**
     * Validates given {@link ClientObjectFactory} class
     *
     * @param clientObjectFactoryClass class implementing {@link ClientObjectFactory}
     * @return true if this factory can produce a {@link BatchEmitter} compatible with given {@link
     * ClientObjectFactory}, false otherwise
     */
    boolean accepts(Class<? extends ClientObjectFactory> clientObjectFactoryClass);

    /**
     * Creates an instance of {@link BatchEmitter}
     *
     * @param batchSize           number of elements in a current batch that should trigger a delivery, regardless of
     *                            the deliveryInterval
     * @param deliveryInterval    number of millis between two time-triggered deliveries, regardless of the batchSize
     * @param clientObjectFactory client-specific objects provider
     * @param failoverPolicy      sink for failed batch items
     * @return T configured and {@link BatchEmitter}
     */
    T createInstance(int batchSize, int deliveryInterval, ClientObjectFactory clientObjectFactory, FailoverPolicy failoverPolicy);

}
