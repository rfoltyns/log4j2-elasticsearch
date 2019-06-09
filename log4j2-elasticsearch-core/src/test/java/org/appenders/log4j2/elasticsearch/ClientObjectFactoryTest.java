package org.appenders.log4j2.elasticsearch;

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

import org.junit.Test;

import java.util.Collection;
import java.util.function.Function;

public class ClientObjectFactoryTest {

    @Test
    public void addOperationHasDefaultImpl() {

        // given
        ClientObjectFactory factory = new ClientObjectFactory() {

            @Override
            public Collection<String> getServerList() {
                return null;
            }

            @Override
            public Object createClient() {
                return null;
            }

            @Override
            public Function createBatchListener(FailoverPolicy failoverPolicy) {
                return null;
            }

            @Override
            public Function createFailureHandler(FailoverPolicy failover) {
                return null;
            }

            @Override
            public BatchOperations createBatchOperations() {
                return null;
            }

            @Override
            public void execute(IndexTemplate indexTemplate) {

            }

        };

        // when
        factory.addOperation(() -> {});

    }

}
