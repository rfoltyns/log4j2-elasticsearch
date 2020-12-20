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

import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.ClientProvider;
import org.appenders.log4j2.elasticsearch.util.SplitUtil;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static org.appenders.core.logging.InternalLogging.setLogger;
import static org.appenders.core.logging.InternalLoggingTest.mockTestLogger;
import static org.appenders.log4j2.elasticsearch.hc.HttpClientProviderTest.TEST_SERVER_URIS;
import static org.appenders.log4j2.elasticsearch.hc.HttpClientProviderTest.createDefaultTestClientProvider;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ClientProviderPoliciesRegistryTest {

    private static final Set<String> SHARED = new HashSet<>(Collections.singleton("shared"));
    private static final Set<String> NEW = new HashSet<>(Collections.singleton("none"));
    private static final Set<String> SERVER_LIST = new HashSet<>(Collections.singleton("serverList"));
    private static final Set<String> SECURITY = new HashSet<>(Collections.singleton("security"));

    @Test
    public void resolvesSharedClientPolicyByDefault() {

        // given
        ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        // when
        ClientProviderPolicy<HttpClient> policy = registry.get(SHARED, createDefaultTestClientProvider());

        // then
        assertNotNull(policy);

    }

    @Test
    public void resolvesNewClientPolicyByDefault() {

        // given
        ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        // when
        ClientProviderPolicy<HttpClient> policy = registry.get(NEW, createDefaultTestClientProvider());

        // then
        assertNotNull(policy);

    }

    @Test
    public void resolvesServerListCopyingPolicyByDefault() {

        // given
        ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        // when
        ClientProviderPolicy<HttpClient> policy = registry.get(SERVER_LIST, createDefaultTestClientProvider());

        // then
        assertNotNull(policy);

    }

    @Test
    public void resolvesSecurityCopyingPolicyByDefault() {

        // given
        ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        // when
        ClientProviderPolicy<HttpClient> policy = registry.get(SECURITY, createDefaultTestClientProvider());

        // then
        assertNotNull(policy);

    }

    @Test
    public void resolvesCustomCopyingPolicyIfRegistered() {

        // given
        ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        String expectedPolicyName = UUID.randomUUID().toString();

        ClientProviderPoliciesRegistry.CopyingConfigPolicy<HttpClient> customPolicy = spy(new ClientProviderPoliciesRegistry.CopyingConfigPolicy<HttpClient>() {
            @Override
            public String getName() {
                return expectedPolicyName;
            }

            @Override
            public void copy(ClientProvider<HttpClient> source, ClientProvider<HttpClient> target) {

            }
        });

        registry.register(customPolicy);

        HttpClientProvider initialTestClientProvider = createDefaultTestClientProvider();
        HttpClientProvider sourceClientConfigProvider = createDefaultTestClientProvider();

        // when
        ClientProviderPolicy<HttpClient> policy = registry.get(
                new HashSet<>(Collections.singletonList(expectedPolicyName)),
                initialTestClientProvider);
        policy.apply(sourceClientConfigProvider);

        // then
        assertNotNull(policy);
        verify(customPolicy).copy(sourceClientConfigProvider, initialTestClientProvider);

    }

    @Test
    public void throwsOnEmptyPolicyList() {

        // given
        ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        Set<String> policies = new HashSet<>();

        // when
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registry.get(policies, createDefaultTestClientProvider()));

        // then
        assertThat(exception.getMessage(),
                equalTo("Policy list must present. Valid policies: " + registry.availablePolicies()));

    }

    @Test
    public void throwsOnNullPolicyList() {

        // given
        ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        Set<String> policies = null;

        // when
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registry.get(policies, createDefaultTestClientProvider()));

        // then
        assertThat(exception.getMessage(),
                equalTo("Policy list must present. Valid policies: " + registry.availablePolicies()));

    }

    @Test
    public void throwsOnNonExistingPolicy() {

        // given
        ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        String expectedPolicyName = UUID.randomUUID().toString();

        Set<String> policies = new HashSet<>();
        policies.add(expectedPolicyName);

        // when
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registry.get(policies, createDefaultTestClientProvider()));

        // then
        assertThat(exception.getMessage(),
                equalTo("Invalid policy specified: [" + expectedPolicyName + "]. Available policies: " + registry.availablePolicies()));

    }

    @Test
    public void throwsIfSharedClientPolicyMixedWithOthers() {

        // given
        ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        Set<String> policies = Stream.concat(SHARED.stream(), SERVER_LIST.stream()).collect(toSet());

        // when
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registry.get(policies, createDefaultTestClientProvider()));

        // then
        assertThat(exception.getMessage(),
                equalTo("Cannot apply other policies when [shared] policy is used"));

    }

    @Test
    public void throwsIfNewClientPolicyMixedWithOthers() {

        // given
        ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        Set<String> policies = Stream.concat(NEW.stream(), SERVER_LIST.stream()).collect(Collectors.toSet());

        // when
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registry.get(policies, createDefaultTestClientProvider()));

        // then
        assertThat(exception.getMessage(),
                equalTo("Cannot apply other policies when [none] policy is used"));

    }

    @Test
    public void sharedClientPolicyIgnoresInitialClientProvider() {

        // given
        ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        HttpClientProvider initialClientProvider = createDefaultTestClientProvider();
        ClientProviderPolicy<HttpClient> policy = registry.get(SHARED, initialClientProvider);

        ClientProvider<HttpClient> sourceClientProvider = createDefaultTestClientProvider();

        // when
        ClientProvider<HttpClient> result = policy.apply(sourceClientProvider);

        // then
        assertNotSame(sourceClientProvider, initialClientProvider);
        assertSame(sourceClientProvider, result);

    }

    @Test
    public void newClientPolicyIgnoresAppliedClientProvider() {

        // given
        ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        HttpClientProvider initialClientProvider = createDefaultTestClientProvider();
        ClientProviderPolicy<HttpClient> policy = registry.get(NEW, initialClientProvider);

        ClientProvider<HttpClient> sourceClientProvider = createDefaultTestClientProvider();

        // when
        ClientProvider<HttpClient> result = policy.apply(sourceClientProvider);

        // then
        assertNotSame(sourceClientProvider, initialClientProvider);
        assertSame(initialClientProvider, result);

    }

    @Test
    public void serverListCopyingPolicyCopiesFromAppliedClientProviderIfInitialIsNull() {

        // given
        ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        HttpClientProvider initialClientProvider = createDefaultTestClientProvider();
        initialClientProvider.getHttpClientFactoryBuilder().withServerList(null);
        ClientProviderPolicy<HttpClient> policy = registry.get(SERVER_LIST, initialClientProvider);

        List<String> expectedServerList = SplitUtil.split(TEST_SERVER_URIS);
        HttpClientProvider sourceClientProvider = createDefaultTestClientProvider();
        sourceClientProvider.getHttpClientFactoryBuilder().withServerList(expectedServerList);

        // when
        HttpClientProvider result = (HttpClientProvider) policy.apply(sourceClientProvider);

        // then
        assertEquals(expectedServerList, result.getHttpClientFactoryBuilder().serverList);

    }

    @Test
    public void serverListCopyingPolicyDoesNotCopyFromAppliedClientProviderIfInitialIsNotNull() {

        // given
        ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        List<String> expectedServerList = SplitUtil.split(TEST_SERVER_URIS);
        HttpClientProvider initialClientProvider = createDefaultTestClientProvider();

        initialClientProvider.getHttpClientFactoryBuilder().withServerList(expectedServerList);

        ClientProviderPolicy<HttpClient> policy = registry.get(SERVER_LIST, initialClientProvider);

        List<String> unexpectedServerList = SplitUtil.split(TEST_SERVER_URIS + "1");
        HttpClientProvider sourceClientProvider = createDefaultTestClientProvider();
        sourceClientProvider.getHttpClientFactoryBuilder().withServerList(unexpectedServerList);

        // when
        HttpClientProvider result = (HttpClientProvider) policy.apply(sourceClientProvider);

        // then
        assertEquals(expectedServerList, result.getHttpClientFactoryBuilder().serverList);

    }

    @Test
    public void securityCopyingPolicyCopiesFromAppliedClientProviderIfInitialIsNull() {

        // given
        ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        HttpClientProvider initialClientProvider = createDefaultTestClientProvider();
        initialClientProvider.getHttpClientFactoryBuilder()
                .withAuth(null)
                .withPlainSocketFactory(null)
                .withSslSocketFactory(null)
                .withHttpIOSessionStrategy(null)
                .withHttpsIOSessionStrategy(null)
                .withDefaultCredentialsProvider(null);

        ClientProviderPolicy<HttpClient> policy = registry.get(SECURITY, initialClientProvider);

        Security expectedAuth = SecurityTest.createTestBuilder().build();
        PlainConnectionSocketFactory expectedPlainConnectionSocketFactory = mock(PlainConnectionSocketFactory.class);
        SSLConnectionSocketFactory expectedSSLConnectionSocketFactory = mock(SSLConnectionSocketFactory.class);
        SchemeIOSessionStrategy expectedHttpIOSessionStrategy = mock(SchemeIOSessionStrategy.class);
        SchemeIOSessionStrategy expectedHttpsIOSessionStrategy = mock(SchemeIOSessionStrategy.class);
        CredentialsProvider expectedDefaultCredentialsProvider = mock(CredentialsProvider.class);

        HttpClientProvider sourceClientProvider = createDefaultTestClientProvider();
        sourceClientProvider.getHttpClientFactoryBuilder()
                .withAuth(expectedAuth)
                .withPlainSocketFactory(expectedPlainConnectionSocketFactory)
                .withSslSocketFactory(expectedSSLConnectionSocketFactory)
                .withHttpIOSessionStrategy(expectedHttpIOSessionStrategy)
                .withHttpsIOSessionStrategy(expectedHttpsIOSessionStrategy)
                .withDefaultCredentialsProvider(expectedDefaultCredentialsProvider);

        // when
        HttpClientProvider result = (HttpClientProvider) policy.apply(sourceClientProvider);

        // then
        assertEquals(expectedAuth, result.getHttpClientFactoryBuilder().auth);
        assertEquals(expectedPlainConnectionSocketFactory, result.getHttpClientFactoryBuilder().plainSocketFactory);
        assertEquals(expectedSSLConnectionSocketFactory, result.getHttpClientFactoryBuilder().sslSocketFactory);
        assertEquals(expectedHttpIOSessionStrategy, result.getHttpClientFactoryBuilder().httpIOSessionStrategy);
        assertEquals(expectedHttpsIOSessionStrategy, result.getHttpClientFactoryBuilder().httpsIOSessionStrategy);
        assertEquals(expectedDefaultCredentialsProvider, result.getHttpClientFactoryBuilder().defaultCredentialsProvider);

    }

    @Test
    public void securityCopyingPolicyDoesNotCopyFromAppliedClientProviderIfInitialIsNotNull() {

        // given
        ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        Security expectedAuth = SecurityTest.createTestBuilder().build();
        PlainConnectionSocketFactory expectedPlainConnectionSocketFactory = mock(PlainConnectionSocketFactory.class);
        SSLConnectionSocketFactory expectedSSLConnectionSocketFactory = mock(SSLConnectionSocketFactory.class);
        SchemeIOSessionStrategy expectedHttpIOSessionStrategy = mock(SchemeIOSessionStrategy.class);
        SchemeIOSessionStrategy expectedHttpsIOSessionStrategy = mock(SchemeIOSessionStrategy.class);
        CredentialsProvider expectedDefaultCredentialsProvider = mock(CredentialsProvider.class);

        HttpClientProvider initialClientProvider = createDefaultTestClientProvider();
        initialClientProvider.getHttpClientFactoryBuilder()
                .withAuth(expectedAuth)
                .withPlainSocketFactory(expectedPlainConnectionSocketFactory)
                .withSslSocketFactory(expectedSSLConnectionSocketFactory)
                .withHttpIOSessionStrategy(expectedHttpIOSessionStrategy)
                .withHttpsIOSessionStrategy(expectedHttpsIOSessionStrategy)
                .withDefaultCredentialsProvider(expectedDefaultCredentialsProvider);

        ClientProviderPolicy<HttpClient> policy = registry.get(SECURITY, initialClientProvider);

        Security unexpectedAuth = SecurityTest.createTestBuilder().build();
        PlainConnectionSocketFactory unexpectedPlainConnectionSocketFactory = mock(PlainConnectionSocketFactory.class);
        SSLConnectionSocketFactory unexpectedSSLConnectionSocketFactory = mock(SSLConnectionSocketFactory.class);
        SchemeIOSessionStrategy unexpectedHttpIOSessionStrategy = mock(SchemeIOSessionStrategy.class);
        SchemeIOSessionStrategy unexpectedHttpsIOSessionStrategy = mock(SchemeIOSessionStrategy.class);
        CredentialsProvider unexpectedDefaultCredentialsProvider = mock(CredentialsProvider.class);

        HttpClientProvider sourceClientProvider = createDefaultTestClientProvider();
        sourceClientProvider.getHttpClientFactoryBuilder()
                .withAuth(unexpectedAuth)
                .withPlainSocketFactory(unexpectedPlainConnectionSocketFactory)
                .withSslSocketFactory(unexpectedSSLConnectionSocketFactory)
                .withHttpIOSessionStrategy(unexpectedHttpIOSessionStrategy)
                .withHttpsIOSessionStrategy(unexpectedHttpsIOSessionStrategy)
                .withDefaultCredentialsProvider(unexpectedDefaultCredentialsProvider);

        // when
        HttpClientProvider result = (HttpClientProvider) policy.apply(sourceClientProvider);

        // then
        assertEquals(expectedAuth, result.getHttpClientFactoryBuilder().auth);
        assertEquals(expectedPlainConnectionSocketFactory, result.getHttpClientFactoryBuilder().plainSocketFactory);
        assertEquals(expectedSSLConnectionSocketFactory, result.getHttpClientFactoryBuilder().sslSocketFactory);
        assertEquals(expectedHttpIOSessionStrategy, result.getHttpClientFactoryBuilder().httpIOSessionStrategy);
        assertEquals(expectedHttpsIOSessionStrategy, result.getHttpClientFactoryBuilder().httpsIOSessionStrategy);
        assertEquals(expectedDefaultCredentialsProvider, result.getHttpClientFactoryBuilder().defaultCredentialsProvider);

    }

    @Test
    public void canMixMultipleCopyingPolicies() {

        // given
        ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        HttpClientProvider initialClientProvider = createDefaultTestClientProvider();
        initialClientProvider.getHttpClientFactoryBuilder()
                .withServerList(null)
                .withAuth(null)
                .withPlainSocketFactory(null)
                .withSslSocketFactory(null)
                .withHttpIOSessionStrategy(null)
                .withHttpsIOSessionStrategy(null)
                .withDefaultCredentialsProvider(null);

        Set<String> policies = Stream.concat(SECURITY.stream(), SERVER_LIST.stream()).collect(toSet());
        ClientProviderPolicy<HttpClient> policy = registry.get(policies, initialClientProvider);

        List<String> expectedServerList = SplitUtil.split(TEST_SERVER_URIS);
        Security expectedAuth = SecurityTest.createTestBuilder().build();
        PlainConnectionSocketFactory expectedPlainConnectionSocketFactory = mock(PlainConnectionSocketFactory.class);
        SSLConnectionSocketFactory expectedSSLConnectionSocketFactory = mock(SSLConnectionSocketFactory.class);
        SchemeIOSessionStrategy expectedHttpIOSessionStrategy = mock(SchemeIOSessionStrategy.class);
        SchemeIOSessionStrategy expectedHttpsIOSessionStrategy = mock(SchemeIOSessionStrategy.class);
        CredentialsProvider expectedDefaultCredentialsProvider = mock(CredentialsProvider.class);

        HttpClientProvider sourceClientProvider = createDefaultTestClientProvider();
        sourceClientProvider.getHttpClientFactoryBuilder()
                .withServerList(expectedServerList)
                .withAuth(expectedAuth)
                .withPlainSocketFactory(expectedPlainConnectionSocketFactory)
                .withSslSocketFactory(expectedSSLConnectionSocketFactory)
                .withHttpIOSessionStrategy(expectedHttpIOSessionStrategy)
                .withHttpsIOSessionStrategy(expectedHttpsIOSessionStrategy)
                .withDefaultCredentialsProvider(expectedDefaultCredentialsProvider);

        // when
        HttpClientProvider result = (HttpClientProvider) policy.apply(sourceClientProvider);

        // then
        assertEquals(expectedServerList, result.getHttpClientFactoryBuilder().serverList);
        assertEquals(expectedAuth, result.getHttpClientFactoryBuilder().auth);
        assertEquals(expectedPlainConnectionSocketFactory, result.getHttpClientFactoryBuilder().plainSocketFactory);
        assertEquals(expectedSSLConnectionSocketFactory, result.getHttpClientFactoryBuilder().sslSocketFactory);
        assertEquals(expectedHttpIOSessionStrategy, result.getHttpClientFactoryBuilder().httpIOSessionStrategy);
        assertEquals(expectedHttpsIOSessionStrategy, result.getHttpClientFactoryBuilder().httpsIOSessionStrategy);
        assertEquals(expectedDefaultCredentialsProvider, result.getHttpClientFactoryBuilder().defaultCredentialsProvider);

    }

    @Test
    public void propertiesMapperDoesNotCopyNullSourceValues() {

        // given
        String expectedPropertyName = UUID.randomUUID().toString();
        Supplier<String> source = () -> null;
        Supplier<String> target = () -> null;

        Logger logger = mockTestLogger();

        // when
        ClientProviderPoliciesRegistry.PropertiesMapper.copyProperty(expectedPropertyName, source, target, value -> Assert.fail("Should not copy"));

        //then
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> propertyNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger).debug(logCaptor.capture(), any(), propertyNameCaptor.capture());

        assertThat(logCaptor.getValue(), CoreMatchers.containsString("source value is empty"));
        assertThat(propertyNameCaptor.getValue(), equalTo(expectedPropertyName));

        setLogger(null);

    }

    @Test
    public void propertiesMapperDoesNotCopyBlankSourceStrings() {

        // given
        String expectedPropertyName = UUID.randomUUID().toString();
        Supplier<String> source = () -> " ";
        Supplier<String> target = () -> null;

        Logger logger = mockTestLogger();

        // when
        ClientProviderPoliciesRegistry.PropertiesMapper.copyProperty(expectedPropertyName, source, target, value -> Assert.fail("Should not copy"));

        //then
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> propertyNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger).debug(logCaptor.capture(), any(), propertyNameCaptor.capture());

        assertThat(logCaptor.getValue(), CoreMatchers.containsString("source value is empty"));
        assertThat(propertyNameCaptor.getValue(), equalTo(expectedPropertyName));

        setLogger(null);

    }

    @Test
    public void propertiesMapperDoesNotCopyEmptyCollections() {

        // given
        String expectedPropertyName = UUID.randomUUID().toString();
        Supplier<List<String>> source = ArrayList::new;
        Supplier<List<String>> target = () -> null;

        Logger logger = mockTestLogger();

        // when
        ClientProviderPoliciesRegistry.PropertiesMapper.copyProperty(expectedPropertyName, source, target, value -> Assert.fail("Should not copy"));

        //then
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> propertyNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger).debug(logCaptor.capture(), any(), propertyNameCaptor.capture());

        assertThat(logCaptor.getValue(), CoreMatchers.containsString("source value is empty"));
        assertThat(propertyNameCaptor.getValue(), equalTo(expectedPropertyName));

        setLogger(null);

    }

    @Test
    public void propertiesMapperDoesNotCopyIfTargetStringIsNotBlank() {

        // given
        String expectedPropertyName = UUID.randomUUID().toString();
        Supplier<String> source = () -> UUID.randomUUID().toString();
        Supplier<String> target = () -> UUID.randomUUID().toString();

        Logger logger = mockTestLogger();

        // when
        ClientProviderPoliciesRegistry.PropertiesMapper.copyProperty(expectedPropertyName, source, target, value -> Assert.fail("Should not copy"));

        //then
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> propertyNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger).debug(logCaptor.capture(), any(), propertyNameCaptor.capture());

        assertThat(logCaptor.getValue(), CoreMatchers.containsString("target value is not empty"));
        assertThat(propertyNameCaptor.getValue(), equalTo(expectedPropertyName));

        setLogger(null);

    }

    @Test
    public void propertiesMapperDoesNotCopyIfTargetCollectionIsNotEmpty() {

        // given
        String expectedPropertyName = UUID.randomUUID().toString();
        Supplier<List<String>> source = () -> SplitUtil.split(TEST_SERVER_URIS);
        Supplier<List<String>> target = () -> SplitUtil.split(TEST_SERVER_URIS + "1");

        Logger logger = mockTestLogger();

        // when
        ClientProviderPoliciesRegistry.PropertiesMapper.copyProperty(expectedPropertyName, source, target, value -> Assert.fail("Should not copy"));

        //then
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> propertyNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger).debug(logCaptor.capture(), any(), propertyNameCaptor.capture());

        assertThat(logCaptor.getValue(), CoreMatchers.containsString("target value is not empty"));
        assertThat(propertyNameCaptor.getValue(), equalTo(expectedPropertyName));

        setLogger(null);

    }

}
