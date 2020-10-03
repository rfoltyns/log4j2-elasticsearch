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


import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.appenders.log4j2.elasticsearch.CertInfo;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.appenders.log4j2.elasticsearch.hc.SecurityTest.createDefaultTestObjectBuilder;
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

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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
        Assert.assertNotNull(certInfo);

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

        HttpClientFactory.Builder httpClientFactoryBuilder = spy(createDefaultTestObjectBuilder());

        // when
        certInfo.applyTo(httpClientFactoryBuilder);

        // then
        verify(httpClientFactoryBuilder).withHttpsIOSessionStrategy((SchemeIOSessionStrategy) notNull());
        Assert.assertNotNull(httpClientFactoryBuilder.build().httpIOSessionStrategy);
    }

    @Test
    public void keyPathIsNotAppliedIfNotConfigured() {

        // given
        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("No keyPath provided");

        // when
        createTestCertInfoBuilder()
                .withKeyPath(null)
                .build();

    }

    @Test
    public void builderThrowsIfClientCertPathIsNotConfigured() {

        // given
        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("No clientCertPath provided");

        // when
        createTestCertInfoBuilder()
                .withClientCertPath(null)
                .build();

    }

    @Test
    public void builderThrowsIfCaPathIsNotConfigured() {

        // given
        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("No caPath provided");

        // when
        createTestCertInfoBuilder()
                .withCaPath(null)
                .build();

    }

    @Test
    public void builderThrowsIfCantReadKey() {

        // given
        PEMCertInfo testCertInfo = createTestCertInfoBuilder()
                .withKeyPath(TEST_KEY_PATH_WITH_PASSPHRASE)
                .withKeyPassphrase("")
                .build();

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage(PEMCertInfo.configExceptionMessage);

        // when
        testCertInfo.applyTo(mock(HttpClientFactory.Builder.class));

    }

    private File createInvalidKey() throws IOException {
        File tempFile = File.createTempFile("log4j2-elasticsaarch", "certinfo-test");
        tempFile.deleteOnExit();
        FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
        fileOutputStream.write("cert-test".getBytes());
        fileOutputStream.close();
        return  tempFile;
    }

}
