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
import org.appenders.log4j2.elasticsearch.CertInfo;
import org.junit.jupiter.api.Test;

import static org.appenders.log4j2.elasticsearch.ahc.HttpClientFactoryTest.createDefaultTestHttpClientFactoryBuilder;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class PEMCertInfoPluginTest {

    public static final String TEST_KEY_PATH = System.getProperty("pemCertInfo.keyPath");
    public static final String TEST_KEY_PATH_WITH_PASSPHRASE = System.getProperty("pemCertInfo.keyPathWithPassphrase");
    public static final String TEST_CLIENT_CERT_PATH = System.getProperty("pemCertInfo.clientCertPath");
    public static final String TEST_CA_PATH = System.getProperty("pemCertInfo.caPath");
    public static final String TEST_KEY_PASSPHRASE = System.getProperty("pemCertInfo.keyPassphrase");

    public static PEMCertInfoPlugin.Builder createTestCertInfoBuilder() {
        return PEMCertInfoPlugin.newBuilder()
                .withKeyPath(TEST_KEY_PATH_WITH_PASSPHRASE)
                .withKeyPassphrase(TEST_KEY_PASSPHRASE)
                .withClientCertPath(TEST_CLIENT_CERT_PATH)
                .withCaPath(TEST_CA_PATH);
    }

    @Test
    public void minimalBuilderTest() {

        // given
        final PEMCertInfoPlugin.Builder builder = createTestCertInfoBuilder();

        // when
        final CertInfo<HttpClientFactory.Builder> certInfo = builder.build();

        // then
        assertNotNull(certInfo);

    }

    @Test
    public void paramsArePassedToConfiguredObject() {

        // given
        final PEMCertInfoPlugin certInfo = createTestCertInfoBuilder()
                .withKeyPath(TEST_KEY_PATH)
                .withKeyPassphrase(TEST_KEY_PASSPHRASE)
                .withClientCertPath(TEST_CLIENT_CERT_PATH)
                .withCaPath(TEST_CA_PATH)
                .build();

        final HttpClientFactory.Builder httpClientFactoryBuilder = spy(createDefaultTestHttpClientFactoryBuilder());

        // when
        certInfo.applyTo(httpClientFactoryBuilder);

        // then
        verify(httpClientFactoryBuilder).withSslEngineFactory(notNull());
        assertNotNull(httpClientFactoryBuilder.build().sslEngineFactory);

    }

    @Test
    public void keyPathIsNotAppliedIfNotConfigured() {

        // given
        final PEMCertInfoPlugin.Builder builder = createTestCertInfoBuilder()
                .withKeyPath(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("keyPath"));

    }

    @Test
    public void builderThrowsIfClientCertPathIsNotConfigured() {

        // given
        final PEMCertInfoPlugin.Builder builder = createTestCertInfoBuilder()
                .withClientCertPath(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("clientCertPath"));

    }

    @Test
    public void builderThrowsIfCaPathIsNotConfigured() {

        // given
        final PEMCertInfoPlugin.Builder builder = createTestCertInfoBuilder()
                .withCaPath(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("caPath"));

    }

    @Test
    public void builderThrowsIfCantReadKey() {

        // given
        final PEMCertInfoPlugin testCertInfo = createTestCertInfoBuilder()
                .withKeyPath(TEST_KEY_PATH_WITH_PASSPHRASE)
                .withKeyPassphrase("")
                .build();

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class,
                () -> testCertInfo.applyTo(mock(HttpClientFactory.Builder.class)));

        // then
        assertEquals(PEMCertInfo.configExceptionMessage, exception.getMessage());

    }

}
