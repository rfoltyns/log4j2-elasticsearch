package org.appenders.log4j2.elasticsearch.ahc.jmh;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2022 Rafal Foltynski
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

import org.openjdk.jmh.infra.Blackhole;

public class AsyncBatchEmitterWithBatchOperationsMain {

    public static void main(final String[] args) {
        final AsyncBatchEmitterWithBatchOperationsTest test = new AsyncBatchEmitterWithBatchOperationsTest();
        test.itemPoolSize = 1;
        test.itemSizeInBytes = 1024;

        test.prepare();

        final int limit = 1000000000;
        final Blackhole blackhole = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");
        for (int i = 0; i < limit; i++) {
            test.smokeTest(blackhole);
        }

    }
}
