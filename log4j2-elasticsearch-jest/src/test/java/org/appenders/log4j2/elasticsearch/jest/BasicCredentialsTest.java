package org.appenders.log4j2.elasticsearch.jest;

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
