package org.appenders.log4j2.elasticsearch.jest;

/*-
 * #%L
 * log4j-elasticsearch
 * %%
 * Copyright (C) 2018 Rafal Foltynski
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import io.searchbox.client.config.HttpClientConfig;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.appenders.log4j2.elasticsearch.CertInfo;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

import static org.mockito.Matchers.notNull;
import static org.appenders.log4j2.elasticsearch.jest.XPackAuthTest.createDefaultClientConfigBuilder;
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

        HttpClientConfig.Builder clientConfigBuilder = spy(createDefaultClientConfigBuilder());

        // when
        certInfo.applyTo(clientConfigBuilder);

        // then
        verify(clientConfigBuilder).httpsIOSessionStrategy((SchemeIOSessionStrategy) notNull());
        Assert.assertNotNull(clientConfigBuilder.build().getHttpsIOSessionStrategy());
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
        testCertInfo.applyTo(mock(HttpClientConfig.Builder.class));

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
