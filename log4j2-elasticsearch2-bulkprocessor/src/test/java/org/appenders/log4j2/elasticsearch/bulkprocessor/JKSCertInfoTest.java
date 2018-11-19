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
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class JKSCertInfoTest {

    public static final String TEST_KEYSTORE_PATH = "testkeystorePath";
    public static final String TEST_KEYSTORE_PASSWORD = "testKeyStorePassword";
    public static final String TEST_TRUSTSTORE_PATH = "testClientCertPath";
    public static final String TEST_TRUSTSTORE_PASSWORD = "testTruststorePassword";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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
        Assert.assertNotNull(certInfo);

    }

    @Test
    public void sslTransportIsEnabledByDefault() {

        // given
        JKSCertInfo certInfo = JKSCertInfo.newBuilder().build();

        Settings.Builder settings = Settings.builder();

        // when
        certInfo.applyTo(settings);

        // then
        Assert.assertTrue(Boolean.valueOf(settings.get(JKSCertInfo.SHIELD_TRANSPORT_SSL_ENABLED)));

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
        Assert.assertEquals(TEST_KEYSTORE_PATH, settings.get(JKSCertInfo.SHIELD_SSL_KEYSTORE_PATH));
        Assert.assertEquals(TEST_KEYSTORE_PASSWORD, settings.get(JKSCertInfo.SHIELD_SSL_KEYSTORE_PASSWORD));
        Assert.assertEquals(TEST_TRUSTSTORE_PATH, settings.get(JKSCertInfo.SHIELD_SSL_TRUSTSTORE_PATH));
        Assert.assertEquals(TEST_TRUSTSTORE_PASSWORD, settings.get(JKSCertInfo.SHIELD_SSL_TRUSTSTORE_PASSWORD));
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
        Assert.assertNull(settings.get(JKSCertInfo.SHIELD_SSL_KEYSTORE_PATH));

    }

    @Test
    public void builderThrowsIfKeystorePasswordIsSetToNull() {

        // given
        JKSCertInfo.Builder builder= JKSCertInfo.newBuilder()
                .withKeystorePassword(null);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("keystorePassword");

        // when
        builder.build();

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
        Assert.assertEquals(JKSCertInfo.Builder.EMPTY_PASSWORD, settings.get(JKSCertInfo.SHIELD_SSL_KEYSTORE_PASSWORD));

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
        Assert.assertNull(settings.get(JKSCertInfo.SHIELD_SSL_TRUSTSTORE_PATH));

    }

    @Test
    public void builderThrowsIfTruststorePasswordIsSetToNull() {

        // given
        JKSCertInfo.Builder builder= JKSCertInfo.newBuilder()
                .withTruststorePassword(null);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("truststorePassword");

        // when
        builder.build();

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
        Assert.assertEquals(JKSCertInfo.Builder.EMPTY_PASSWORD, settings.get(JKSCertInfo.SHIELD_SSL_KEYSTORE_PASSWORD));

    }

}
