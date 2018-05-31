package org.appenders.log4j2.elasticsearch.bulkprocessor;

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


import org.apache.logging.log4j.core.config.ConfigurationException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class UriParserTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void parserReturnsHostGivenHttpUrl() {

        // given
        String url = "tcp://localhost:8080";
        UriParser uriParser = new UriParser();

        // when
        String parsedHost = uriParser.getHost(url);

        // then
        Assert.assertEquals("localhost", parsedHost);
    }

    @Test
    public void parserReturnsIpGivenHttpUrl() {

        // given
        String url = "tcp://10.120.10.10:8080";
        UriParser uriParser = new UriParser();

        // when
        String parsedHost = uriParser.getHost(url);

        // then
        Assert.assertEquals("10.120.10.10", parsedHost);
    }

    @Test
    public void parserReturnsPortGivenHttpUrl() {

        // given
        String url = "tcp://localhost:8080";
        UriParser uriParser = new UriParser();

        // when
        int parsedPort = uriParser.getPort(url);

        // then
        Assert.assertEquals(8080, parsedPort);
    }

    @Test
    public void parserReturnsPortGivenIpUrl() {

        // given
        String url = "tcp://10.120.10.10:8080";
        UriParser uriParser = new UriParser();

        // when
        int parsedPort = uriParser.getPort(url);

        // then
        Assert.assertEquals(8080, parsedPort);
    }

    @Test
    public void getHostThrowsOnInvalidUri() {

        // given
        String url = "${";
        UriParser uriParser = new UriParser();

        expectedException.expect(ConfigurationException.class);

        // when
        uriParser.getHost(url);
    }

    @Test
    public void getPortThrowsOnInvalidUri() {

        // given
        String url = "%s";
        UriParser uriParser = new UriParser();

        expectedException.expect(ConfigurationException.class);

        // when
        uriParser.getPort(url);

    }

}
