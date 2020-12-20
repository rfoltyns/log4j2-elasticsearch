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

import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.hc.discovery.ServerInfo;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.appenders.core.logging.InternalLoggingTest.mockTestLogger;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ServerPoolTest {

    public static final String TEST_SERVER = "http://localhost:9200";
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void throwsOnNullInitialServerList() {

        // given
        expectedException.expect(IllegalArgumentException.class);

        // when
        new ServerPool(null);

    }

    @Test
    public void throwsOnNullInitialServerWithNoScheme() {

        // given
        expectedException.expect(IllegalArgumentException.class);

        // when
        new ServerPool(Collections.singletonList("localhost:9200"));

    }

    @Test
    public void throwsWhenNoServersAvailableOnGetNext() {

        // given
        System.setProperty("appenders.ServerPool.wait.interval", "1");
        System.setProperty("appenders.ServerPool.wait.retries", "3");

        Logger logger = mockTestLogger();

        ServerPool serverPool = new ServerPool(Collections.emptyList());

        // when
        IllegalStateException exception = assertThrows(IllegalStateException.class, serverPool::getNext);

        // then
        assertThat(exception.getMessage(), containsString("No servers available after 3 retries"));
        verify(logger, times(3)).warn("No servers available");

    }

    @Test
    public void returnsServersInOrder() {

        // given
        List<String> serverList = new ArrayList<>();
        int iterations = new Random().nextInt(50) + 2;
        for (int i = 0; i < iterations; i++) {
            serverList.add("http://localhost:" + (i + 10000));
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

    @Test
    public void returnsNewServersAfterUpdate() {

        // given
        List<String> initialServerList = new ArrayList<>();
        int iterations = new Random().nextInt(50) + 2;
        for (int i = 0; i < iterations; i++) {
            initialServerList.add("http://localhost:" + (i + 10000));
        }

        ServerPool serverPool = new ServerPool(initialServerList);

        List<ServerInfo> updatedServerList = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            updatedServerList.add(new ServerInfo("http://localhost:" + (i + 10100)));
        }

        // when
        List<String> result = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            result.add(serverPool.getNext());
        }

        serverPool.onServerInfo(updatedServerList);

        for (int i = 0; i < iterations; i++) {
            result.add(serverPool.getNext());
        }

        // then
        for (int i = 0; i < iterations; i++) {
            assertEquals(initialServerList.get(i), result.get(i));
        }
        for (int i = 0; i < iterations; i++) {
            assertEquals(updatedServerList.get(i).getResolvedAddress(), result.get(iterations + i));
        }

    }

}
