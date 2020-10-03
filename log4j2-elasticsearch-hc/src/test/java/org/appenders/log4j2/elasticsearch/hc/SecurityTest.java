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
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SecurityTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Captor
    private ArgumentCaptor<HttpClientFactory.Builder> builderArgumentCaptor;

    public static Security.Builder createTestBuilder() {
        return Security.newBuilder()
                .withCredentials(BasicCredentialsTest.createTestBuilder().build())
                .withCertInfo(PEMCertInfoTest.createTestCertInfoBuilder().build());
    }

    public static HttpClientFactory.Builder createDefaultTestObjectBuilder() {
        return new HttpClientFactory.Builder();
    }

    @Test
    public void minimalBuilderTest() {

        // given
        Security.Builder builder = createTestBuilder();

        // when
        Security security = builder.build();

        // then
        Assert.assertNotNull(security);

    }

    @Test
    public void appliesCredentialsIfConfigured() {

        // given
        Credentials<HttpClientFactory.Builder> credentials = Mockito.mock(Credentials.class);

        HttpClientFactory.Builder settingsBuilder = createDefaultTestObjectBuilder();

        Security security = createTestBuilder()
                .withCredentials(credentials)
                .build();

        // when
        security.configure(settingsBuilder);

        // then
        verify(credentials).applyTo(builderArgumentCaptor.capture());
        Assert.assertEquals(settingsBuilder, builderArgumentCaptor.getValue());

    }

    @Test
    public void failsIfCredentialsNotConfigured() {

        // given
        Security.Builder builder = createTestBuilder()
                .withCredentials(null);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("credentials");

        // when
        builder.build();

    }

    @Test
    public void appliesCertInfoIfConfigured() {

        // given
        CertInfo<HttpClientFactory.Builder> certInfo = Mockito.mock(CertInfo.class);

        HttpClientFactory.Builder settingsBuilder = createDefaultTestObjectBuilder();

        Security security = createTestBuilder()
                .withCertInfo(certInfo)
                .build();

        // when
        security.configure(settingsBuilder);

        // then
        verify(certInfo).applyTo(builderArgumentCaptor.capture());
        Assert.assertEquals(settingsBuilder, builderArgumentCaptor.getValue());

    }

    @Test
    public void doesntApplyCertInfoIfNotConfigured() {

        // given
        Security auth = createTestBuilder()
                .withCertInfo(null)
                .build();

        HttpClientFactory.Builder settingsBuilder = Mockito.spy(createDefaultTestObjectBuilder());

        // when
        auth.configure(settingsBuilder);

        // then
        verify(settingsBuilder, never()).withSslSocketFactory(any());
        verify(settingsBuilder, never()).withHttpsIOSessionStrategy(any());
    }

}
