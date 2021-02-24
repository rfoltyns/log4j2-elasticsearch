package org.appenders.log4j2.elasticsearch.hc;

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


import org.appenders.log4j2.elasticsearch.CertInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;

import java.security.Security;

import static org.appenders.log4j2.elasticsearch.hc.HttpClientFactoryTest.createDefaultTestHttpClientFactoryBuilder;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
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
        CertInfo<HttpClientFactory.Builder> certInfo = builder.build();

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

        HttpClientFactory.Builder httpClientFactoryBuilder = spy(createDefaultTestHttpClientFactoryBuilder());

        // when
        certInfo.applyTo(httpClientFactoryBuilder);

        // then
        verify(httpClientFactoryBuilder).withHttpsIOSessionStrategy(notNull());
        assertNotNull(httpClientFactoryBuilder.build().httpIOSessionStrategy);

    }

    @Test
    public void keyPathIsNotAppliedIfNotConfigured() {

        // given
        PEMCertInfo.Builder builder = createTestCertInfoBuilder()
                .withKeyPath(null);

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("keyPath"));

    }

    @Test
    public void builderThrowsIfClientCertPathIsNotConfigured() {

        // given
        PEMCertInfo.Builder builder = createTestCertInfoBuilder()
                .withClientCertPath(null);

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("clientCertPath"));

    }

    @Test
    public void builderThrowsIfCaPathIsNotConfigured() {

        // given
        PEMCertInfo.Builder builder = createTestCertInfoBuilder()
                .withCaPath(null);

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("caPath"));

    }

    @Test
    public void builderThrowsIfCantReadKey() {

        // given
        PEMCertInfo testCertInfo = createTestCertInfoBuilder()
                .withKeyPath(TEST_KEY_PATH_WITH_PASSPHRASE)
                .withKeyPassphrase("")
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> testCertInfo.applyTo(mock(HttpClientFactory.Builder.class)));

        // then
        assertEquals(PEMCertInfo.configExceptionMessage, exception.getMessage());

    }

    @Test
    public void addsBouncyCastleProviderIfNotLoaded() {

        // given
        java.security.Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);

        PEMCertInfo certInfo = createTestCertInfoBuilder()
                .withKeyPath(TEST_KEY_PATH)
                .withKeyPassphrase(TEST_KEY_PASSPHRASE)
                .withClientCertPath(TEST_CLIENT_CERT_PATH)
                .withCaPath(TEST_CA_PATH)
                .build();

        HttpClientFactory.Builder clientConfigBuilder = spy(createDefaultTestHttpClientFactoryBuilder());

        assertNull(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME));

        // when
        certInfo.applyTo(clientConfigBuilder);

        // then
        assertNotNull(BouncyCastleProvider.PROVIDER_NAME);

    }

}
