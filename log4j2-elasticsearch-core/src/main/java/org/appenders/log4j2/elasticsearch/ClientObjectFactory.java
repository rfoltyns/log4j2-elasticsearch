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

import java.util.Collection;
import java.util.function.Function;

/**
 * Factory for client-specific objects:
 * <ul>
 * <li> the client itself
 * <li> batch items creators (client-specific batch objects)
 * <li> batch processing listeners
 * </ul>
 * <p>
 * Implementations of this class MUST provide a set of client- and {@link BatchEmitter}-compatible objects
 *
 * @param <CLIENT_TYPE> type of client object produced by this factory
 * @param <BATCH_TYPE> type of batch objects handled by the client
 */
public interface ClientObjectFactory<CLIENT_TYPE, BATCH_TYPE> {

    String ELEMENT_TYPE = "objectFactory";

    /**
     * @return Collection of configured addresses
     */
    Collection<String> getServerList();

    /**
     * @return CLIENT_TYPE Fully configured client
     */
    CLIENT_TYPE createClient();

    /**
     * Listener that MUST accept and send prepared batch and handle the exceptions
     * @param failoverPolicy sink for failed batch items
     * @return prepared batch handler
     */
    Function<BATCH_TYPE, Boolean> createBatchListener(FailoverPolicy failoverPolicy);

    /**
     * Failed batch handler. SHOULD deliver the batch to alternate target or provided failover policy
     * @param failover optional failover strategy
     * @return prepared failed batch handler
     */
    Function<BATCH_TYPE, Boolean> createFailureHandler(FailoverPolicy failover);

    /**
     * @return batch items creator
     */
    BatchOperations<BATCH_TYPE> createBatchOperations();

    /**
     * Updates target with index template
     */
    void execute(IndexTemplate indexTemplate);

}
