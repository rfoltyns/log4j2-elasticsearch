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

import org.apache.logging.log4j.core.config.ConfigurationException;
import org.appenders.log4j2.elasticsearch.CertInfo;
import org.appenders.log4j2.elasticsearch.Credentials;
import org.elasticsearch.common.settings.Settings;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class XPackAuthTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Captor
    private ArgumentCaptor<Settings.Builder> builderArgumentCaptor;

    public static XPackAuth.Builder createTestBuilder() {
        return XPackAuth.newBuilder()
                .withCredentials(PlainCredentialsTest.createTestBuilder().build())
                .withCertInfo(PEMCertInfoTest.createTestCertInfoBuilder().build());
    }

    @Test
    public void minimalBuilderTest() {

        // given
        XPackAuth.Builder builder = createTestBuilder();

        // when
        XPackAuth xPackAuth = builder.build();

        // then
        Assert.assertNotNull(xPackAuth);

    }

    @Test
    public void appliesCredentialsIfConfigured() {

        // given
        Credentials<Settings.Builder> credentials = Mockito.mock(Credentials.class);

        Settings.Builder settingsBuilder = Settings.builder();

        XPackAuth xPackAuth = createTestBuilder()
                .withCredentials(credentials)
                .build();

        // when
        xPackAuth.configure(settingsBuilder);

        // then
        Mockito.verify(credentials).applyTo(builderArgumentCaptor.capture());
        Assert.assertEquals(settingsBuilder, builderArgumentCaptor.getValue());

    }

    @Test
    public void failsIfCredentialsNotConfigured() {

        // given
        XPackAuth.Builder builder = createTestBuilder()
                .withCredentials(null);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("credentials");

        // when
        builder.build();

    }

    @Test
    public void appliesCertInfoIfConfigured() {

        // given
        CertInfo<Settings.Builder> certInfo = Mockito.mock(CertInfo.class);

        Settings.Builder settingsBuilder = Settings.builder();

        XPackAuth xPackAuth = createTestBuilder()
                .withCertInfo(certInfo)
                .build();

        // when
        xPackAuth.configure(settingsBuilder);

        // then
        Mockito.verify(certInfo).applyTo(builderArgumentCaptor.capture());
        Assert.assertEquals(settingsBuilder, builderArgumentCaptor.getValue());

    }

    @Test
    public void throwsIfCertInfoNotConfigured() {

        // given
        XPackAuth.Builder builder = createTestBuilder()
                .withCertInfo(null);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("certInfo");

        // when
        builder.build();

    }

}
