package org.appenders.log4j2.elasticsearch.ahc;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2022 Rafal Foltynski
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


import org.appenders.log4j2.elasticsearch.CertInfo;
import org.appenders.log4j2.elasticsearch.Credentials;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.appenders.log4j2.elasticsearch.ahc.HttpClientFactoryTest.createDefaultTestHttpClientFactoryBuilder;
import static org.appenders.log4j2.elasticsearch.ahc.PEMCertInfoTest.createTestCertInfoBuilder;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SecurityTest {

    @Captor
    private ArgumentCaptor<HttpClientFactory.Builder> builderArgumentCaptor;

    public static Security.Builder createDefaultTestSecurityBuilder() {
        return new Security.Builder()
                .withCredentials(BasicCredentialsTest.createTestBuilder().build())
                .withCertInfo(createTestCertInfoBuilder().build());
    }

    @Test
    public void minimalBuilderTest() {

        // given
        final Security.Builder builder = createDefaultTestSecurityBuilder();

        // when
        final Security security = builder.build();

        // then
        assertNotNull(security);

    }

    @Test
    public void appliesCredentialsIfConfigured() {

        // given
        final Credentials<HttpClientFactory.Builder> credentials = spy(new DummyCredentials());

        final HttpClientFactory.Builder settingsBuilder = createDefaultTestHttpClientFactoryBuilder();

        final Security security = createDefaultTestSecurityBuilder()
                .withCredentials(credentials)
                .build();

        // when
        security.configure(settingsBuilder);

        // then
        verify(credentials).applyTo(builderArgumentCaptor.capture());
        assertEquals(settingsBuilder, builderArgumentCaptor.getValue());

    }

    @Test
    public void failsIfCredentialsNotConfigured() {

        // given
        final Security.Builder builder = createDefaultTestSecurityBuilder()
                .withCredentials(null);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("credentials"));

    }

    @Test
    public void appliesCertInfoIfConfigured() {

        // given
        final CertInfo<HttpClientFactory.Builder> certInfo = spy(new DummyCertInfo());

        final HttpClientFactory.Builder settingsBuilder = createDefaultTestHttpClientFactoryBuilder();

        final Security security = createDefaultTestSecurityBuilder()
                .withCertInfo(certInfo)
                .build();

        // when
        security.configure(settingsBuilder);

        // then
        verify(certInfo).applyTo(builderArgumentCaptor.capture());
        assertEquals(settingsBuilder, builderArgumentCaptor.getValue());

    }

    @Test
    public void doesntApplyCertInfoIfNotConfigured() {

        // given
        final Security auth = createDefaultTestSecurityBuilder()
                .withCertInfo(null)
                .build();

        final HttpClientFactory.Builder settingsBuilder = spy(createDefaultTestHttpClientFactoryBuilder());

        // when
        auth.configure(settingsBuilder);

        // then
        verify(settingsBuilder, never()).withSslEngineFactory(any());
    }

    private static class DummyCredentials implements Credentials<HttpClientFactory.Builder> {
        @Override
        public void applyTo(final HttpClientFactory.Builder clientSettings) {
        }
    }

    private static class DummyCertInfo implements CertInfo<HttpClientFactory.Builder> {
        @Override
        public void applyTo(final HttpClientFactory.Builder clientConfig) {
        }

    }

}
