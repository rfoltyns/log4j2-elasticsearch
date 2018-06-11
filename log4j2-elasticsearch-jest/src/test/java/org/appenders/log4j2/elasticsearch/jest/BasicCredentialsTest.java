package org.appenders.log4j2.elasticsearch.jest;

/*-
 * #%L
 * log4j-elasticsearch
 * %%
 * Copyright (C) 2018 Rafal Foltynski
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

import io.searchbox.client.config.HttpClientConfig;
import org.apache.http.client.CredentialsProvider;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.hamcrest.core.AnyOf;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.appenders.log4j2.elasticsearch.jest.XPackAuthTest.createDefaultClientConfigBuilder;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class BasicCredentialsTest {

    private static final String TEST_USER = "test_user";
    private static final String TEST_PASSWORD = "changeme";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    public static BasicCredentials.Builder createTestBuilder() {
        return BasicCredentials.newBuilder()
                .withUsername(TEST_USER)
                .withPassword(TEST_PASSWORD);
    }

    @Test
    public void minimalBuilderTest() {

        // given
        BasicCredentials.Builder builder = createTestBuilder();

        // when
        BasicCredentials certInfo = builder.build();

        // then
        Assert.assertNotNull(certInfo);

    }


    @Test
    public void throwsWhenBothParamsAreNull() {

        // given
        BasicCredentials.Builder builder = createTestBuilder()
                .withUsername(null)
                .withPassword(null);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage(AnyOf.anyOf(
                StringContains.containsString("username"),
                StringContains.containsString("password"))
        );

        // when
        builder.build();

    }

    @Test
    public void throwsWhenUsernameIsNull() {

        // given
        BasicCredentials.Builder builder = createTestBuilder()
                .withUsername(null);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("username");

        // when
        builder.build();

    }

    @Test
    public void throwsWhenPasswordIsNull() {

        // given
        BasicCredentials.Builder builder = createTestBuilder()
                .withPassword(null);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("password");

        // when
        builder.build();

    }


    @Test
    public void objectIsConfiguredWhenAllParamsAreSet() {

        // given
        BasicCredentials BasicCredentials = createTestBuilder()
                .withUsername(TEST_USER)
                .withPassword(TEST_PASSWORD)
                .build();

        HttpClientConfig.Builder settings = spy(createDefaultClientConfigBuilder());

        // when
        BasicCredentials.applyTo(settings);

        // then
        verify(settings).credentialsProvider((CredentialsProvider) notNull());

    }
}
