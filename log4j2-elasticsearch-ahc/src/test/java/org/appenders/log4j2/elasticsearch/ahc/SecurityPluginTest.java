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
import org.appenders.log4j2.elasticsearch.Credentials;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.appenders.log4j2.elasticsearch.ahc.HttpClientFactoryTest.createDefaultTestHttpClientFactoryBuilder;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SecurityPluginTest {

    public static SecurityPlugin.Builder createTestBuilder() {
        return SecurityPlugin.newBuilder()
                .withCredentials(BasicCredentialsTest.createTestBuilder().build())
                .withCertInfo(PEMCertInfoTest.createTestCertInfoBuilder().build());
    }

    @Test
    public void minimalBuilderTest() {

        // given
        final SecurityPlugin.Builder builder = createTestBuilder();

        // when
        final Security security = builder.build();

        // then
        assertNotNull(security);

    }

    @Test
    public void appliesCredentialsIfConfigured() {

        // given
        final Credentials<HttpClientFactory.Builder> credentials = mock(Credentials.class);

        final HttpClientFactory.Builder settingsBuilder = createDefaultTestHttpClientFactoryBuilder();

        final SecurityPlugin security = createTestBuilder()
                .withCredentials(credentials)
                .build();

        // when
        security.configure(settingsBuilder);

        // then
        final ArgumentCaptor<HttpClientFactory.Builder> captor = ArgumentCaptor.forClass(HttpClientFactory.Builder.class);
        verify(credentials).applyTo(captor.capture());
        assertEquals(settingsBuilder, captor.getValue());

    }

    @Test
    public void failsIfCredentialsNotConfigured() {

        // given
        final SecurityPlugin.Builder builder = createTestBuilder()
                .withCredentials(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("credentials"));

    }

}
