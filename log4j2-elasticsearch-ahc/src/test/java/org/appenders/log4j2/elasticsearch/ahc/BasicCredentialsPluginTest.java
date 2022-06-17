package org.appenders.log4j2.elasticsearch.ahc;

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


import org.apache.logging.log4j.core.config.ConfigurationException;
import org.junit.jupiter.api.Test;

import static org.appenders.log4j2.elasticsearch.ahc.HttpClientFactoryTest.createDefaultTestHttpClientFactoryBuilder;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class BasicCredentialsPluginTest {

    private static final String TEST_USER = "test_user";
    private static final String TEST_PASSWORD = "changeme";

    public static BasicCredentialsPlugin.Builder createTestBuilder() {
        return BasicCredentialsPlugin.newBuilder()
                .withUsername(TEST_USER)
                .withPassword(TEST_PASSWORD);
    }

    @Test
    public void minimalBuilderTest() {

        // given
        final BasicCredentialsPlugin.Builder builder = createTestBuilder();

        // when
        final BasicCredentialsPlugin certInfo = builder.build();

        // then
        assertNotNull(certInfo);

    }

    @Test
    public void throwsWhenUsernameIsNull() {

        // given
        final BasicCredentialsPlugin.Builder builder = createTestBuilder()
                .withUsername(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("username"));

    }

    @Test
    public void throwsWhenPasswordIsNull() {

        // given
        final BasicCredentialsPlugin.Builder builder = createTestBuilder()
                .withPassword(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("password"));

    }


    @Test
    public void objectIsConfiguredWhenAllParamsAreSet() {

        // given
        final BasicCredentialsPlugin plugin = createTestBuilder()
                .withUsername(TEST_USER)
                .withPassword(TEST_PASSWORD)
                .build();

        final HttpClientFactory.Builder settings = spy(createDefaultTestHttpClientFactoryBuilder());

        // when
        plugin.applyTo(settings);

        // then
        verify(settings).withRealm(notNull());

    }

}
