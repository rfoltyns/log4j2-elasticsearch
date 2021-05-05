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


import org.appenders.log4j2.elasticsearch.CertInfo;
import org.elasticsearch.common.settings.Settings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PEMCertInfoTest {

    public static final String TEST_KEY_PATH = "testKeyPath";
    public static final String TEST_CLIENT_CERT_PATH = "testClientCertPath";
    public static final String TEST_CA_PATH = "testCaPath";
    public static final String TEST_KEY_PASSPHRASE = "testKeyPassphrase";

    public static PEMCertInfo.Builder createTestCertInfoBuilder() {
        return PEMCertInfo.newBuilder()
                .withKeyPath(TEST_KEY_PATH)
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
    public void sslTransportIsEnabledByDefault() {

        // given
        PEMCertInfo certInfo = PEMCertInfo.newBuilder().build();

        Settings.Builder settings = Settings.builder();

        // when
        certInfo.applyTo(settings);

        // then
        assertTrue(Boolean.valueOf(settings.get(PEMCertInfo.XPACK_SECURITY_TRANSPORT_SSL_ENABLED)));

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

        Settings.Builder settings = Settings.builder();

        // when
        certInfo.applyTo(settings);

        // then
        assertEquals(TEST_KEY_PATH, settings.get(PEMCertInfo.XPACK_SSL_KEY));
        assertEquals(TEST_KEY_PASSPHRASE, settings.get(PEMCertInfo.XPACK_SSL_KEY_PASSPHRASE));
        assertEquals(TEST_CLIENT_CERT_PATH, settings.get(PEMCertInfo.XPACK_SSL_CERTIFICATE));
        assertEquals(TEST_CA_PATH, settings.get(PEMCertInfo.XPACK_SSL_CERTIFICATE_AUTHORITIES));

    }

    @Test
    public void keyPathIsNotAppliedIfNotConfigured() {

        // given
        PEMCertInfo certInfo = PEMCertInfo.newBuilder()
                .withKeyPath(null)
                .build();

        Settings.Builder settings = Settings.builder();

        // when
        certInfo.applyTo(settings);

        // then
        assertNull(settings.get(PEMCertInfo.XPACK_SSL_KEY));

    }

    @Test
    public void clientCertPathIsNotAppliedIfNotConfigured() {

        // given
        PEMCertInfo certInfo = PEMCertInfo.newBuilder()
                .withClientCertPath(null)
                .build();

        Settings.Builder settings = Settings.builder();

        // when
        certInfo.applyTo(settings);

        // then
        assertNull(settings.get(PEMCertInfo.XPACK_SSL_CERTIFICATE));

    }

    @Test
    public void caPathIsNotAppliedIfNotConfigured() {

        // given
        PEMCertInfo certInfo = PEMCertInfo.newBuilder()
                .withCaPath(null)
                .build();

        Settings.Builder settings = Settings.builder();

        // when
        certInfo.applyTo(settings);

        // then
        assertNull(settings.get(PEMCertInfo.XPACK_SSL_CERTIFICATE_AUTHORITIES));

    }

    @Test
    public void keyPasswordIsNotAppliedIfNotConfigured() {

        // given
        PEMCertInfo certInfo = PEMCertInfo.newBuilder()
                .withKeyPassphrase(null)
                .build();

        Settings.Builder settings = Settings.builder();

        // when
        certInfo.applyTo(settings);

        // then
        assertNull(settings.get(PEMCertInfo.XPACK_SSL_KEY_PASSPHRASE));

    }

}
