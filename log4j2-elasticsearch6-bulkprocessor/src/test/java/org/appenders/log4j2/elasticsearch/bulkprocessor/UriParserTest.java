package org.appenders.log4j2.elasticsearch.bulkprocessor;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2018 Rafal Foltynski
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
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UriParserTest {

    @Test
    public void parserReturnsHostGivenHttpUrl() {

        // given
        String url = "tcp://localhost:8080";
        UriParser uriParser = new UriParser();

        // when
        String parsedHost = uriParser.getHost(url);

        // then
        assertEquals("localhost", parsedHost);

    }

    @Test
    public void parserReturnsIpGivenHttpUrl() {

        // given
        String url = "tcp://10.120.10.10:8080";
        UriParser uriParser = new UriParser();

        // when
        String parsedHost = uriParser.getHost(url);

        // then
        assertEquals("10.120.10.10", parsedHost);

    }

    @Test
    public void parserReturnsPortGivenHttpUrl() {

        // given
        String url = "tcp://localhost:8080";
        UriParser uriParser = new UriParser();

        // when
        int parsedPort = uriParser.getPort(url);

        // then
        assertEquals(8080, parsedPort);

    }

    @Test
    public void parserReturnsPortGivenIpUrl() {

        // given
        String url = "tcp://10.120.10.10:8080";
        UriParser uriParser = new UriParser();

        // when
        int parsedPort = uriParser.getPort(url);

        // then
        assertEquals(8080, parsedPort);

    }

    @Test
    public void getHostThrowsOnInvalidUri() {

        // given
        String url = "${";
        UriParser uriParser = new UriParser();

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, () -> uriParser.getHost(url));

        // then
        assertThat(exception.getMessage(), containsString("Illegal character in path at index 1: ${"));

    }

    @Test
    public void getPortThrowsOnInvalidUri() {

        // given
        String url = "%s";
        UriParser uriParser = new UriParser();

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, () -> uriParser.getPort(url));

        // then
        assertThat(exception.getMessage(), equalTo("java.net.URISyntaxException: Malformed escape pair at index 0: %s"));

    }

}
