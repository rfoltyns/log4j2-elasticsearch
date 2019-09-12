package org.appenders.log4j2.elasticsearch.hc;

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

import org.apache.logging.log4j.core.config.ConfigurationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class ServerPoolTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void throwsOnEmptyInitialServerList() {

        // given
        ArrayList<String> serverList = new ArrayList<>();

        expectedException.expect(ConfigurationException.class);

        // when
        new ServerPool(serverList);

    }

    @Test
    public void throwsOnNullInitialServerList() {

        // given
        expectedException.expect(ConfigurationException.class);

        // when
        new ServerPool(null);

    }

    @Test
    public void returnsServersInOrder() {

        // given
        List<String> serverList = new ArrayList<>();
        int iterations = new Random().nextInt(50) + 2;
        for (int i = 0; i < iterations; i++) {
            serverList.add(UUID.randomUUID().toString());
        }

        ServerPool serverPool = new ServerPool(serverList);

        // when
        List<String> result = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            result.add(serverPool.getNext());
        }

        // then
        for (int i = 0; i < iterations; i++) {
            assertEquals(serverList.get(i), result.get(i));
        }

    }

}
