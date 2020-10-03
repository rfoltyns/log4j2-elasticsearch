package org.appenders.log4j2.elasticsearch.jest;

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


import io.searchbox.client.config.HttpClientConfig;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.appenders.log4j2.elasticsearch.CertInfo;
import org.appenders.log4j2.elasticsearch.Credentials;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class XPackAuthTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Captor
    private ArgumentCaptor<HttpClientConfig.Builder> builderArgumentCaptor;

    public static XPackAuth.Builder createTestBuilder() {
        return XPackAuth.newBuilder()
                .withCredentials(BasicCredentialsTest.createTestBuilder().build())
                .withCertInfo(PEMCertInfoTest.createTestCertInfoBuilder().build());
    }

    public static HttpClientConfig.Builder createDefaultClientConfigBuilder() {
        return new HttpClientConfig.Builder("localhost");
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
        Credentials<HttpClientConfig.Builder> credentials = Mockito.mock(Credentials.class);

        HttpClientConfig.Builder settingsBuilder = createDefaultClientConfigBuilder();

        XPackAuth xPackAuth = createTestBuilder()
                .withCredentials(credentials)
                .build();

        // when
        xPackAuth.configure(settingsBuilder);

        // then
        verify(credentials).applyTo(builderArgumentCaptor.capture());
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
        CertInfo<HttpClientConfig.Builder> certInfo = Mockito.mock(CertInfo.class);

        HttpClientConfig.Builder settingsBuilder = createDefaultClientConfigBuilder();

        XPackAuth xPackAuth = createTestBuilder()
                .withCertInfo(certInfo)
                .build();

        // when
        xPackAuth.configure(settingsBuilder);

        // then
        verify(certInfo).applyTo(builderArgumentCaptor.capture());
        Assert.assertEquals(settingsBuilder, builderArgumentCaptor.getValue());

    }

    @Test
    public void doesntApplyCertInfoIfNotConfigured() {

        // given
        XPackAuth auth = createTestBuilder()
                .withCertInfo(null)
                .build();

        HttpClientConfig.Builder settingsBuilder = spy(createDefaultClientConfigBuilder());

        // when
        auth.configure(settingsBuilder);

        // then
        verify(settingsBuilder, never()).sslSocketFactory(any());
        verify(settingsBuilder, never()).httpsIOSessionStrategy(any());
    }

}
