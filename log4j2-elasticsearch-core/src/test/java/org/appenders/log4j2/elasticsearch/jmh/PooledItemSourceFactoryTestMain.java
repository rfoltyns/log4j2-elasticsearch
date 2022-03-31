package org.appenders.log4j2.elasticsearch.jmh;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2021 Rafal Foltynski
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

public class PooledItemSourceFactoryTestMain {

    public static void main(String[] args) {

        final PooledItemSourceFactoryTest test = new PooledItemSourceFactoryTest();
        test.itemSizeInBytes = 8192;
        test.poolSize = 10000;

        test.prepare();

        final int limit = 1000000000;
        final Blackhole blackhole = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");
        for (int i = 0; i < limit; i++) {
            test.smokeTest(blackhole);
        }
    }

}
