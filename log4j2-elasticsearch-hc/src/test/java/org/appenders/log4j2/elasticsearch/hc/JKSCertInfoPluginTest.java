package org.appenders.log4j2.elasticsearch.hc;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2020 Rafal Foltynski
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
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import static org.appenders.log4j2.elasticsearch.hc.HttpClientFactoryTest.createDefaultTestHttpClientFactoryBuilder;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class JKSCertInfoPluginTest {

    public static final String TEST_KEYSTORE_PATH = System.getProperty("jksCertInfo.keystorePath");
    public static final String TEST_KEYSTORE_PASSWORD = System.getProperty("jksCertInfo.keystorePassword");
    public static final String TEST_TRUSTSTORE_PATH = System.getProperty("jksCertInfo.truststorePath");
    public static final String TEST_TRUSTSTORE_PASSWORD = System.getProperty("jksCertInfo.truststorePassword");

    public static JKSCertInfoPlugin.Builder createTestCertInfoBuilder() {
        return JKSCertInfoPlugin.newBuilder()
                .withKeystorePath(TEST_KEYSTORE_PATH)
                .withTruststorePath(TEST_TRUSTSTORE_PATH);
    }

    @Test
    public void minimalBuilderTest() {

        // given
        JKSCertInfoPlugin.Builder builder = createTestCertInfoBuilder();

        // when
        CertInfo<HttpClientFactory.Builder> certInfo = builder.build();

        // then
        Assert.assertNotNull(certInfo);

    }

    @Test
    public void paramsArePassedToConfiguredObject() {

        // given
        JKSCertInfoPlugin certInfo = createTestCertInfoBuilder()
                .withKeystorePath(TEST_KEYSTORE_PATH)
                .withKeystorePassword(TEST_KEYSTORE_PASSWORD)
                .withTruststorePath(TEST_TRUSTSTORE_PATH)
                .withTruststorePassword(TEST_TRUSTSTORE_PASSWORD)
                .build();

        HttpClientFactory.Builder httpClientFactoryBuilder = Mockito.spy(createDefaultTestHttpClientFactoryBuilder());

        // when
        certInfo.applyTo(httpClientFactoryBuilder);

        // then
        verify(httpClientFactoryBuilder).withHttpsIOSessionStrategy(notNull());
        Assert.assertNotNull(httpClientFactoryBuilder.build().httpsIOSessionStrategy);
    }

    @Test
    public void builderThrowsIfKeystorePathIsNotConfigured() {

        // given
        JKSCertInfoPlugin.Builder builder = createTestCertInfoBuilder()
                .withKeystorePath(null);

        // when
        ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("keystorePath"));

    }

    @Test
    public void builderThrowsIfKeystorePasswordIsSetToNull() {

        // given
        JKSCertInfoPlugin.Builder builder= createTestCertInfoBuilder()
                .withKeystorePassword(null);

        // when
        ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("keystorePassword"));

    }

    @Test
    public void builderThrowsIfTruststorePathIsNotConfigured() {

        // given
        JKSCertInfoPlugin.Builder builder = createTestCertInfoBuilder()
                .withTruststorePath(null);


        // when
        ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("truststorePath"));

    }

    @Test
    public void builderThrowsIfTruststorePasswordIsSetToNull() {

        // given
        JKSCertInfoPlugin.Builder builder= createTestCertInfoBuilder()
                .withTruststorePassword(null);

        // when
        ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("truststorePassword"));

    }

    @Test
    public void builderThrowsIfKeyIsInvalid() throws IOException {

        // given
        File invalidKey = createInvalidKey();

        JKSCertInfoPlugin testCertInfo = createTestCertInfoBuilder()
                .withKeystorePath(invalidKey.getAbsolutePath())
                .build();

        // when
        ConfigurationException exception = assertThrows(ConfigurationException.class,
                () -> testCertInfo.applyTo(mock(HttpClientFactory.Builder.class)));

        // then
        assertThat(exception.getMessage(), containsString(PEMCertInfo.configExceptionMessage));

    }

    private File createInvalidKey() throws IOException {
        File tempFile = File.createTempFile("log4j2-elasticsearch", "certInfo-test");
        tempFile.deleteOnExit();
        FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
        fileOutputStream.write("cert-test".getBytes());
        fileOutputStream.close();
        return  tempFile;
    }
}
