package org.appenders.log4j2.elasticsearch.bulkprocessor;

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

import org.appenders.log4j2.elasticsearch.CertInfo;
import org.elasticsearch.common.settings.Settings;
import org.junit.Assert;
import org.junit.Test;

public class PEMCertInfoTest {

    public static final String TEST_KEY_PATH = "testKeyPath";
    public static final String TEST_CLIENT_CERT_PATH = "testClientCertPath";
    public static final String TEST_CA_PATH = "testCaPath";

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
        Assert.assertNotNull(certInfo);

    }

    @Test
    public void sslTransportIsEnabledByDefault() {

        // given
        PEMCertInfo certInfo = PEMCertInfo.newBuilder().build();

        Settings.Builder settings = Settings.builder();

        // when
        certInfo.applyTo(settings);

        // then
        Assert.assertTrue(Boolean.valueOf(settings.get(PEMCertInfo.XPACK_SECURITY_TRANSPORT_SSL_ENABLED)));

    }

    @Test
    public void paramsArePassedToConfiguredObject() {

        // given
        PEMCertInfo certInfo = createTestCertInfoBuilder()
                .withKeyPath(TEST_KEY_PATH)
                .withClientCertPath(TEST_CLIENT_CERT_PATH)
                .withCaPath(TEST_CA_PATH)
                .build();

        Settings.Builder settings = Settings.builder();

        // when
        certInfo.applyTo(settings);

        // then
        Assert.assertEquals(TEST_KEY_PATH, settings.get(PEMCertInfo.XPACK_SSL_KEY));
        Assert.assertEquals(TEST_CLIENT_CERT_PATH, settings.get(PEMCertInfo.XPACK_SSL_CERTIFICATE));
        Assert.assertEquals(TEST_CA_PATH, settings.get(PEMCertInfo.XPACK_SSL_CERTIFICATE_AUTHORITIES));
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
        Assert.assertNull(settings.get(PEMCertInfo.XPACK_SSL_KEY));

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
        Assert.assertNull(settings.get(PEMCertInfo.XPACK_SSL_CERTIFICATE));

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
        Assert.assertNull(settings.get(PEMCertInfo.XPACK_SSL_CERTIFICATE_AUTHORITIES));

    }
}
