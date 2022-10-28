package org.appenders.log4j2.elasticsearch.ahc.discovery;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NodesResponseTest {

    private static final int OFFSET = 100;

    private final Random random = new Random();

    @Test
    public void isSucceededOnNonNullNodes() throws IOException {

        // given
        final int expectedResponseCode = random.nextInt(500) + OFFSET;

        final InputStream response = ClassLoader.getSystemResourceAsStream("nodes-7.json");
        final ObjectReader reader = new ObjectMapper().readerFor(NodesResponse.class);

        final NodesResponse result = reader.readValue(response);
        assertEquals(0, result.getResponseCode());

        // when
        result.withResponseCode(expectedResponseCode);

        // then
        assertEquals(expectedResponseCode, result.getResponseCode());
        assertTrue(result.isSucceeded());
        assertNull(result.getErrorMessage());

    }

    @Test
    public void isNotSucceededOnNullNodes() {

        // given
        final String expectedErrorMessage = UUID.randomUUID().toString();
        final int expectedResponseCode = random.nextInt(500) + OFFSET;

        final NodesResponse response = new NodesResponse(null);

        // when
        response.withResponseCode(expectedResponseCode);

        response.withErrorMessage(expectedErrorMessage);
        // then
        assertEquals(expectedResponseCode, response.getResponseCode());
        assertFalse(response.isSucceeded());
        assertNotNull(response.getErrorMessage());

    }

    @Test
    public void canDeserializeNodesHttp() throws IOException {

        // given
        final InputStream response = ClassLoader.getSystemResourceAsStream("nodes-7.json");
        final ObjectReader reader = new ObjectMapper().readerFor(NodesResponse.class);

        final List<String> expectedAddresses = Arrays.asList("127.0.0.1:9200", "127.0.0.1:9201", "127.0.0.1:9202", "127.0.0.1:9203");

        // when
        final NodesResponse result = reader.readValue(response);

        // then
        assertThat(result.getNodes().values(), everyItem(new BaseMatcher<NodeInfo>() {
            @Override
            public boolean matches(final Object item) {
                final NodeInfo info = (NodeInfo)item;
                return info.getHttpPublishAddress() != null
                        && expectedAddresses.contains(info.getHttpPublishAddress().getPublishAddress());
            }

            @Override
            public void describeTo(final Description description) {
            }
        }));
    }

    @Test
    public void canDeserializeNodesHttpPublishAddress() throws IOException {

        // given
        final InputStream response = ClassLoader.getSystemResourceAsStream("nodes-7.json");
        final ObjectReader reader = new ObjectMapper().readerFor(NodesResponse.class);

        // when
        final NodesResponse result = reader.readValue(response);

        // then
        assertThat(result.getNodes().values(), everyItem(new BaseMatcher<NodeInfo>() {
            @Override
            public boolean matches(final Object item) {
                final NodeInfo info = (NodeInfo)item;
                return info.getHttpPublishAddress().getPublishAddress() != null;
            }

            @Override
            public void describeTo(final Description description) {
            }
        }));
    }

}
