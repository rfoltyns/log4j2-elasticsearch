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
import org.appenders.log4j2.elasticsearch.CertInfo;
import org.elasticsearch.common.settings.Settings;
import org.junit.jupiter.api.Test;

import static java.lang.Boolean.parseBoolean;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JKSCertInfoTest {

    public static final String TEST_KEYSTORE_PATH = "testkeystorePath";
    public static final String TEST_KEYSTORE_PASSWORD = "testKeyStorePassword";
    public static final String TEST_TRUSTSTORE_PATH = "testClientCertPath";
    public static final String TEST_TRUSTSTORE_PASSWORD = "testTruststorePassword";

    public static JKSCertInfo.Builder createTestCertInfoBuilder() {
        return JKSCertInfo.newBuilder()
                .withKeystorePath(TEST_KEYSTORE_PATH)
                .withTruststorePath(TEST_TRUSTSTORE_PATH);
    }

    @Test
    public void minimalBuilderTest() {

        // given
        JKSCertInfo.Builder builder = createTestCertInfoBuilder();

        // when
        CertInfo certInfo = builder.build();

        // then
        assertNotNull(certInfo);

    }

    @Test
    public void sslTransportIsEnabledByDefault() {

        // given
        JKSCertInfo certInfo = JKSCertInfo.newBuilder().build();

        Settings.Builder settings = Settings.builder();

        // when
        certInfo.applyTo(settings);

        // then
        assertTrue(parseBoolean(settings.get(JKSCertInfo.SHIELD_TRANSPORT_SSL_ENABLED)));

    }

    @Test
    public void paramsArePassedToConfiguredObject() {

        // given
        JKSCertInfo certInfo = createTestCertInfoBuilder()
                .withKeystorePath(TEST_KEYSTORE_PATH)
                .withKeystorePassword(TEST_KEYSTORE_PASSWORD)
                .withTruststorePath(TEST_TRUSTSTORE_PATH)
                .withTruststorePassword(TEST_TRUSTSTORE_PASSWORD)
                .build();

        Settings.Builder settings = Settings.builder();

        // when
        certInfo.applyTo(settings);

        // then
        assertEquals(TEST_KEYSTORE_PATH, settings.get(JKSCertInfo.SHIELD_SSL_KEYSTORE_PATH));
        assertEquals(TEST_KEYSTORE_PASSWORD, settings.get(JKSCertInfo.SHIELD_SSL_KEYSTORE_PASSWORD));
        assertEquals(TEST_TRUSTSTORE_PATH, settings.get(JKSCertInfo.SHIELD_SSL_TRUSTSTORE_PATH));
        assertEquals(TEST_TRUSTSTORE_PASSWORD, settings.get(JKSCertInfo.SHIELD_SSL_TRUSTSTORE_PASSWORD));

    }

    @Test
    public void keystorePathIsNotAppliedIfNotConfigured() {

        // given
        JKSCertInfo certInfo = JKSCertInfo.newBuilder()
                .withKeystorePath(null)
                .build();

        Settings.Builder settings = Settings.builder();

        // when
        certInfo.applyTo(settings);

        // then
        assertNull(settings.get(JKSCertInfo.SHIELD_SSL_KEYSTORE_PATH));

    }

    @Test
    public void builderThrowsIfKeystorePasswordIsSetToNull() {

        // given
        JKSCertInfo.Builder builder= JKSCertInfo.newBuilder()
                .withKeystorePassword(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), equalTo("No keystorePassword provided for " + JKSCertInfo.PLUGIN_NAME));

    }

    @Test
    public void keystorePasswordIsSetToDefaultIfNotConfigured() {

        // given
        JKSCertInfo certInfo = JKSCertInfo.newBuilder()
                .build();

        Settings.Builder settings = Settings.builder();

        // when
        certInfo.applyTo(settings);

        // then
        assertEquals(JKSCertInfo.Builder.EMPTY_PASSWORD, settings.get(JKSCertInfo.SHIELD_SSL_KEYSTORE_PASSWORD));

    }

    @Test
    public void truststorePathIsNotAppliedIfNotConfigured() {

        // given
        JKSCertInfo certInfo = JKSCertInfo.newBuilder()
                .withTruststorePath(null)
                .build();

        Settings.Builder settings = Settings.builder();

        // when
        certInfo.applyTo(settings);

        // then
        assertNull(settings.get(JKSCertInfo.SHIELD_SSL_TRUSTSTORE_PATH));

    }

    @Test
    public void builderThrowsIfTruststorePasswordIsSetToNull() {

        // given
        JKSCertInfo.Builder builder= JKSCertInfo.newBuilder()
                .withTruststorePassword(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), equalTo("No truststorePassword provided for " + JKSCertInfo.PLUGIN_NAME));

    }

    @Test
    public void truststorePasswordIsSetToDefaultIfNotConfigured() {

        // given
        JKSCertInfo certInfo = JKSCertInfo.newBuilder()
                .build();

        Settings.Builder settings = Settings.builder();

        // when
        certInfo.applyTo(settings);

        // then
        assertEquals(JKSCertInfo.Builder.EMPTY_PASSWORD, settings.get(JKSCertInfo.SHIELD_SSL_KEYSTORE_PASSWORD));

    }

}
