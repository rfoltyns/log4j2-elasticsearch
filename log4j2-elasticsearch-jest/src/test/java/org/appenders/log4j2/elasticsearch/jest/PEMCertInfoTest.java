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
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.appenders.log4j2.elasticsearch.CertInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;

import java.security.Security;

import static org.appenders.log4j2.elasticsearch.jest.XPackAuthTest.createDefaultClientConfigBuilder;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class PEMCertInfoTest {

    public static final String TEST_KEY_PATH = System.getProperty("pemCertInfo.keyPath");
    public static final String TEST_KEY_PATH_WITH_PASSPHRASE = System.getProperty("pemCertInfo.keyPathWithPassphrase");
    public static final String TEST_CLIENT_CERT_PATH = System.getProperty("pemCertInfo.clientCertPath");
    public static final String TEST_CA_PATH = System.getProperty("pemCertInfo.caPath");
    public static final String TEST_KEY_PASSPHRASE = System.getProperty("pemCertInfo.keyPassphrase");

    public static PEMCertInfo.Builder createTestCertInfoBuilder() {
        return PEMCertInfo.newBuilder()
                .withKeyPath(TEST_KEY_PATH_WITH_PASSPHRASE)
                .withKeyPassphrase(TEST_KEY_PASSPHRASE)
                .withClientCertPath(TEST_CLIENT_CERT_PATH)
                .withCaPath(TEST_CA_PATH);
    }

    @Test
    public void minimalBuilderTest() {

        // given
        PEMCertInfo.Builder builder = createTestCertInfoBuilder();

        // when
        CertInfo certInfo = builder.build();

        // then
        assertNotNull(certInfo);

    }

    @Test
    public void paramsArePassedToConfiguredObject() {

        // given
        PEMCertInfo certInfo = createTestCertInfoBuilder()
                .withKeyPath(TEST_KEY_PATH)
                .withKeyPassphrase(TEST_KEY_PASSPHRASE)
                .withClientCertPath(TEST_CLIENT_CERT_PATH)
                .withCaPath(TEST_CA_PATH)
                .build();

        HttpClientConfig.Builder clientConfigBuilder = spy(createDefaultClientConfigBuilder());

        // when
        certInfo.applyTo(clientConfigBuilder);

        // then
        verify(clientConfigBuilder).httpsIOSessionStrategy(notNull());
        assertNotNull(clientConfigBuilder.build().getHttpsIOSessionStrategy());

    }

    @Test
    public void keyPathIsNotAppliedIfNotConfigured() {

        // given
        final PEMCertInfo.Builder builder = createTestCertInfoBuilder()
                .withKeyPath(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No keyPath provided"));

    }

    @Test
    public void builderThrowsIfClientCertPathIsNotConfigured() {

        // given
        final PEMCertInfo.Builder builder = createTestCertInfoBuilder()
                .withClientCertPath(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No clientCertPath provided"));

    }

    @Test
    public void builderThrowsIfCaPathIsNotConfigured() {

        // given
        final PEMCertInfo.Builder builder = createTestCertInfoBuilder()
                .withCaPath(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No caPath provided"));

    }

    @Test
    public void builderThrowsIfCantReadKey() {

        // given
        PEMCertInfo testCertInfo = createTestCertInfoBuilder()
                .withKeyPath(TEST_KEY_PATH_WITH_PASSPHRASE)
                .withKeyPassphrase("")
                .build();

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, () -> testCertInfo.applyTo(mock(HttpClientConfig.Builder.class)));

        // then
        assertThat(exception.getMessage(), containsString(PEMCertInfo.configExceptionMessage));

    }

    @Test
    public void addsBouncyCastleProviderIfNotLoaded() {

        // given
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);

        PEMCertInfo certInfo = createTestCertInfoBuilder()
                .withKeyPath(TEST_KEY_PATH)
                .withKeyPassphrase(TEST_KEY_PASSPHRASE)
                .withClientCertPath(TEST_CLIENT_CERT_PATH)
                .withCaPath(TEST_CA_PATH)
                .build();

        HttpClientConfig.Builder clientConfigBuilder = spy(createDefaultClientConfigBuilder());

        assertNull(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME));

        // when
        certInfo.applyTo(clientConfigBuilder);

        // then
        assertNotNull(BouncyCastleProvider.PROVIDER_NAME);

    }

}
