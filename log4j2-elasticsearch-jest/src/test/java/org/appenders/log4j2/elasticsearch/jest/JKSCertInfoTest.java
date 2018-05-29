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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.appenders.log4j2.elasticsearch.jest.JKSCertInfo.PLUGIN_NAME;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class JKSCertInfoTest {

    public static final String TEST_KEYSTORE_PATH = System.getProperty("jksCertInfo.keystorePath");
    public static final String TEST_KEYSTORE_PASSWORD = System.getProperty("jksCertInfo.keystorePassword");
    public static final String TEST_TRUSTSTORE_PATH = System.getProperty("jksCertInfo.truststorePath");
    public static final String TEST_TRUSTSTORE_PASSWORD = System.getProperty("jksCertInfo.truststorePassword");

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    public static JKSCertInfo.Builder createTestCertInfoBuilder() {
        return JKSCertInfo.newBuilder()
                .withKeystorePath(TEST_KEYSTORE_PATH)
                .withTruststorePath(TEST_TRUSTSTORE_PATH);
    }

    public static HttpClientConfig.Builder createDefaultClientConfigBuilder() {
        return new HttpClientConfig.Builder("localhost");
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
    public void paramsArePassedToConfiguredObject() {

        // given
        JKSCertInfo certInfo = createTestCertInfoBuilder()
                .withKeystorePath(TEST_KEYSTORE_PATH)
                .withKeystorePassword(TEST_KEYSTORE_PASSWORD)
                .withTruststorePath(TEST_TRUSTSTORE_PATH)
                .withTruststorePassword(TEST_TRUSTSTORE_PASSWORD)
                .build();

        HttpClientConfig.Builder clientConfigBuilder = spy(createDefaultClientConfigBuilder());

        // when
        certInfo.applyTo(clientConfigBuilder);

        // then
        verify(clientConfigBuilder).httpsIOSessionStrategy((SchemeIOSessionStrategy) notNull());
        Assert.assertNotNull(clientConfigBuilder.build().getHttpsIOSessionStrategy());
    }

    @Test
    public void builderThrowsIfKeystorePathIsNotConfigured() {

        // given
        JKSCertInfo.Builder builder = createTestCertInfoBuilder()
                .withKeystorePath(null);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("No keystorePath provided for " + PLUGIN_NAME);

        // when
        builder.build();

    }

    @Test
    public void builderThrowsIfKeystorePasswordIsSetToNull() {

        // given
        JKSCertInfo.Builder builder= createTestCertInfoBuilder()
                .withKeystorePassword(null);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("keystorePassword");

        // when
        builder.build();

    }

    @Test
    public void builderThrowsIfTruststorePathIsNotConfigured() {

        // given
        JKSCertInfo.Builder builder = createTestCertInfoBuilder()
                .withTruststorePath(null);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("No truststorePath provided for " + PLUGIN_NAME);

        // when
        builder.build();

    }


    @Test
    public void builderThrowsIfTruststorePasswordIsSetToNull() {

        // given
        JKSCertInfo.Builder builder= createTestCertInfoBuilder()
                .withTruststorePassword(null);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("truststorePassword");

        // when
        builder.build();

    }
    @Test
    public void builderThrowsIfKeyIsInvalid() throws IOException {

        // given
        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage(PEMCertInfo.configExceptionMessage);

        File invalidKey = createInvalidKey();

        JKSCertInfo testCertInfo = createTestCertInfoBuilder()
                .withKeystorePath(invalidKey.getAbsolutePath())
                .build();

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
