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

import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.ClientProvider;
import org.appenders.log4j2.elasticsearch.util.SplitUtil;
import org.asynchttpclient.Realm;
import org.asynchttpclient.SslEngineFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
import static org.appenders.log4j2.elasticsearch.ahc.HttpClientProviderTest.TEST_SERVER_URIS;
import static org.appenders.log4j2.elasticsearch.ahc.HttpClientProviderTest.createDefaultTestClientProvider;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        final ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        // when
        final ClientProviderPolicy<HttpClient> policy = registry.get(SHARED, createDefaultTestClientProvider());

        // then
        assertNotNull(policy);

    }

    @Test
    public void resolvesNewClientPolicyByDefault() {

        // given
        final ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        // when
        final ClientProviderPolicy<HttpClient> policy = registry.get(NEW, createDefaultTestClientProvider());

        // then
        assertNotNull(policy);

    }

    @Test
    public void resolvesServerListCopyingPolicyByDefault() {

        // given
        final ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        // when
        final ClientProviderPolicy<HttpClient> policy = registry.get(SERVER_LIST, createDefaultTestClientProvider());

        // then
        assertNotNull(policy);

    }

    @Test
    public void resolvesSecurityCopyingPolicyByDefault() {

        // given
        final ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        // when
        final ClientProviderPolicy<HttpClient> policy = registry.get(SECURITY, createDefaultTestClientProvider());

        // then
        assertNotNull(policy);

    }

    @Test
    public void resolvesCustomCopyingPolicyIfRegistered() {

        // given
        final ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        final String expectedPolicyName = UUID.randomUUID().toString();

        final ClientProviderPoliciesRegistry.CopyingConfigPolicy<HttpClient> customPolicy = spy(new ClientProviderPoliciesRegistry.CopyingConfigPolicy<HttpClient>() {
            @Override
            public String getName() {
                return expectedPolicyName;
            }

            @Override
            public void copy(final ClientProvider<HttpClient> source, final ClientProvider<HttpClient> target) {

            }
        });

        registry.register(customPolicy);

        final HttpClientProvider initialTestClientProvider = createDefaultTestClientProvider();
        final HttpClientProvider sourceClientConfigProvider = createDefaultTestClientProvider();

        // when
        final ClientProviderPolicy<HttpClient> policy = registry.get(
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
        final ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        final Set<String> policies = new HashSet<>();

        // when
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registry.get(policies, createDefaultTestClientProvider()));

        // then
        assertThat(exception.getMessage(),
                equalTo("Policy list must present. Valid policies: " + registry.availablePolicies()));

    }

    @Test
    public void throwsOnNullPolicyList() {

        // given
        final ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        // when
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registry.get(null, createDefaultTestClientProvider()));

        // then
        assertThat(exception.getMessage(),
                equalTo("Policy list must present. Valid policies: " + registry.availablePolicies()));

    }

    @Test
    public void throwsOnNonExistingPolicy() {

        // given
        final ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        final String expectedPolicyName = UUID.randomUUID().toString();

        final Set<String> policies = new HashSet<>();
        policies.add(expectedPolicyName);

        // when
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registry.get(policies, createDefaultTestClientProvider()));

        // then
        assertThat(exception.getMessage(),
                equalTo("Invalid policy specified: [" + expectedPolicyName + "]. Available policies: " + registry.availablePolicies()));

    }

    @Test
    public void throwsIfSharedClientPolicyMixedWithOthers() {

        // given
        final ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        final Set<String> policies = Stream.concat(SHARED.stream(), SERVER_LIST.stream()).collect(toSet());

        // when
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registry.get(policies, createDefaultTestClientProvider()));

        // then
        assertThat(exception.getMessage(),
                equalTo("Cannot apply other policies when [shared] policy is used"));

    }

    @Test
    public void throwsIfNewClientPolicyMixedWithOthers() {

        // given
        final ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        final Set<String> policies = Stream.concat(NEW.stream(), SERVER_LIST.stream()).collect(Collectors.toSet());

        // when
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registry.get(policies, createDefaultTestClientProvider()));

        // then
        assertThat(exception.getMessage(),
                equalTo("Cannot apply other policies when [none] policy is used"));

    }

    @Test
    public void sharedClientPolicyIgnoresInitialClientProvider() {

        // given
        final ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        final HttpClientProvider initialClientProvider = createDefaultTestClientProvider();
        final ClientProviderPolicy<HttpClient> policy = registry.get(SHARED, initialClientProvider);

        final ClientProvider<HttpClient> sourceClientProvider = createDefaultTestClientProvider();

        // when
        final ClientProvider<HttpClient> result = policy.apply(sourceClientProvider);

        // then
        assertNotSame(sourceClientProvider, initialClientProvider);
        assertSame(sourceClientProvider, result);

    }

    @Test
    public void newClientPolicyIgnoresAppliedClientProvider() {

        // given
        final ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        final HttpClientProvider initialClientProvider = createDefaultTestClientProvider();
        final ClientProviderPolicy<HttpClient> policy = registry.get(NEW, initialClientProvider);

        final ClientProvider<HttpClient> sourceClientProvider = createDefaultTestClientProvider();

        // when
        final ClientProvider<HttpClient> result = policy.apply(sourceClientProvider);

        // then
        assertNotSame(sourceClientProvider, initialClientProvider);
        assertSame(initialClientProvider, result);

    }

    @Test
    public void serverListCopyingPolicyCopiesFromAppliedClientProviderIfInitialIsNull() {

        // given
        final ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        final HttpClientProvider initialClientProvider = createDefaultTestClientProvider();
        initialClientProvider.getHttpClientFactoryBuilder().withServerList(null);
        final ClientProviderPolicy<HttpClient> policy = registry.get(SERVER_LIST, initialClientProvider);

        final List<String> expectedServerList = SplitUtil.split(TEST_SERVER_URIS);
        final HttpClientProvider sourceClientProvider = createDefaultTestClientProvider();
        sourceClientProvider.getHttpClientFactoryBuilder().withServerList(expectedServerList);

        // when
        final HttpClientProvider result = (HttpClientProvider) policy.apply(sourceClientProvider);

        // then
        assertEquals(expectedServerList, result.getHttpClientFactoryBuilder().serverList);

    }

    @Test
    public void serverListCopyingPolicyDoesNotCopyFromAppliedClientProviderIfInitialIsNotNull() {

        // given
        final ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        final List<String> expectedServerList = SplitUtil.split(TEST_SERVER_URIS);
        final HttpClientProvider initialClientProvider = createDefaultTestClientProvider();

        initialClientProvider.getHttpClientFactoryBuilder().withServerList(expectedServerList);

        final ClientProviderPolicy<HttpClient> policy = registry.get(SERVER_LIST, initialClientProvider);

        final List<String> unexpectedServerList = SplitUtil.split(TEST_SERVER_URIS + "1");
        final HttpClientProvider sourceClientProvider = createDefaultTestClientProvider();
        sourceClientProvider.getHttpClientFactoryBuilder().withServerList(unexpectedServerList);

        // when
        final HttpClientProvider result = (HttpClientProvider) policy.apply(sourceClientProvider);

        // then
        assertEquals(expectedServerList, result.getHttpClientFactoryBuilder().serverList);

    }

    @Test
    public void securityCopyingPolicyCopiesFromAppliedClientProviderIfInitialIsNull() {

        // given
        final ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        final HttpClientProvider initialClientProvider = createDefaultTestClientProvider();
        initialClientProvider.getHttpClientFactoryBuilder()
                .withAuth(null)
                .withSslEngineFactory(null)
                .withRealm(null);

        final ClientProviderPolicy<HttpClient> policy = registry.get(SECURITY, initialClientProvider);

        final Security expectedAuth = SecurityTest.createDefaultTestSecurityBuilder().build();
        final Realm expectedRealm = new Realm.Builder(BasicCredentialsTest.TEST_USER, BasicCredentialsTest.TEST_PASSWORD)
                .setScheme(Realm.AuthScheme.BASIC)
                .build();
        final SslEngineFactory expectedSSLEngineFactory = mock(SslEngineFactory.class);

        final HttpClientProvider sourceClientProvider = createDefaultTestClientProvider();
        sourceClientProvider.getHttpClientFactoryBuilder()
                .withAuth(expectedAuth)
                .withSslEngineFactory(expectedSSLEngineFactory)
                .withRealm(expectedRealm);

        // when
        final HttpClientProvider result = (HttpClientProvider) policy.apply(sourceClientProvider);

        // then
        assertEquals(expectedAuth, result.getHttpClientFactoryBuilder().auth);
        assertEquals(expectedRealm, result.getHttpClientFactoryBuilder().realm);
        assertEquals(expectedSSLEngineFactory, result.getHttpClientFactoryBuilder().sslEngineFactory);

    }

    @Test
    public void securityCopyingPolicyDoesNotCopyFromAppliedClientProviderIfInitialIsNotNull() {

        // given
        final ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        final Security expectedAuth = SecurityTest.createDefaultTestSecurityBuilder().build();
        final Realm expectedRealm = new Realm.Builder(BasicCredentialsTest.TEST_USER, BasicCredentialsTest.TEST_PASSWORD)
                .setScheme(Realm.AuthScheme.BASIC)
                .build();
        final SslEngineFactory expectedSSLFactory = mock(SslEngineFactory.class);

        final HttpClientProvider initialClientProvider = createDefaultTestClientProvider();
        initialClientProvider.getHttpClientFactoryBuilder()
                .withAuth(expectedAuth)
                .withRealm(expectedRealm)
                .withSslEngineFactory(expectedSSLFactory);

        final ClientProviderPolicy<HttpClient> policy = registry.get(SECURITY, initialClientProvider);

        final Security unexpectedAuth = SecurityTest.createDefaultTestSecurityBuilder().build();
        final Realm unexpectedRealm = new Realm.Builder(BasicCredentialsTest.TEST_USER, BasicCredentialsTest.TEST_PASSWORD)
                .setScheme(Realm.AuthScheme.BASIC)
                .build();
        final SslEngineFactory unexpectedSSLFactory = mock(SslEngineFactory.class);

        final HttpClientProvider sourceClientProvider = createDefaultTestClientProvider();
        sourceClientProvider.getHttpClientFactoryBuilder()
                .withAuth(unexpectedAuth)
                .withRealm(unexpectedRealm)
                .withSslEngineFactory(unexpectedSSLFactory);

        // when
        final HttpClientProvider result = (HttpClientProvider) policy.apply(sourceClientProvider);

        // then
        assertEquals(expectedAuth, result.getHttpClientFactoryBuilder().auth);
        assertEquals(expectedRealm, result.getHttpClientFactoryBuilder().realm);
        assertEquals(expectedSSLFactory, result.getHttpClientFactoryBuilder().sslEngineFactory);

    }

    @Test
    public void canMixMultipleCopyingPolicies() {

        // given
        final ClientProviderPoliciesRegistry registry = new ClientProviderPoliciesRegistry();

        final HttpClientProvider initialClientProvider = createDefaultTestClientProvider();
        initialClientProvider.getHttpClientFactoryBuilder()
                .withServerList(null)
                .withAuth(null)
                .withRealm(null)
                .withSslEngineFactory(null);

        final Set<String> policies = Stream.concat(SECURITY.stream(), SERVER_LIST.stream()).collect(toSet());
        final ClientProviderPolicy<HttpClient> policy = registry.get(policies, initialClientProvider);

        final List<String> expectedServerList = SplitUtil.split(TEST_SERVER_URIS);
        final Security expectedAuth = SecurityTest.createDefaultTestSecurityBuilder().build();
        final Realm expectedRealm = new Realm.Builder(BasicCredentialsTest.TEST_USER, BasicCredentialsTest.TEST_PASSWORD)
                .setScheme(Realm.AuthScheme.BASIC)
                .build();
        final SslEngineFactory expectedSSLEngineFactory = mock(SslEngineFactory.class);

        final HttpClientProvider sourceClientProvider = createDefaultTestClientProvider();
        sourceClientProvider.getHttpClientFactoryBuilder()
                .withServerList(expectedServerList)
                .withAuth(expectedAuth)
                .withRealm(expectedRealm)
                .withSslEngineFactory(expectedSSLEngineFactory);

        // when
        final HttpClientProvider result = (HttpClientProvider) policy.apply(sourceClientProvider);

        // then
        assertEquals(expectedServerList, result.getHttpClientFactoryBuilder().serverList);
        assertEquals(expectedAuth, result.getHttpClientFactoryBuilder().auth);
        assertEquals(expectedRealm, result.getHttpClientFactoryBuilder().realm);
        assertEquals(expectedSSLEngineFactory, result.getHttpClientFactoryBuilder().sslEngineFactory);

    }

    @Test
    public void propertiesMapperDoesNotCopyNullSourceValues() {

        // given
        final String expectedPropertyName = UUID.randomUUID().toString();
        final Supplier<String> source = () -> null;
        final Supplier<String> target = () -> null;

        final Logger logger = mockTestLogger();

        // when
        ClientProviderPoliciesRegistry.PropertiesMapper.copyProperty(expectedPropertyName, source, target, value -> Assertions.fail("Should not copy"));

        //then
        final ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<String> propertyNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger).debug(logCaptor.capture(), any(), propertyNameCaptor.capture());

        assertThat(logCaptor.getValue(), containsString("source value is empty"));
        assertThat(propertyNameCaptor.getValue(), equalTo(expectedPropertyName));

        setLogger(null);

    }

    @Test
    public void propertiesMapperDoesNotCopyBlankSourceStrings() {

        // given
        final String expectedPropertyName = UUID.randomUUID().toString();
        final Supplier<String> source = () -> " ";
        final Supplier<String> target = () -> null;

        final Logger logger = mockTestLogger();

        // when
        ClientProviderPoliciesRegistry.PropertiesMapper.copyProperty(expectedPropertyName, source, target, value -> Assertions.fail("Should not copy"));

        //then
        final ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<String> propertyNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger).debug(logCaptor.capture(), any(), propertyNameCaptor.capture());

        assertThat(logCaptor.getValue(), containsString("source value is empty"));
        assertThat(propertyNameCaptor.getValue(), equalTo(expectedPropertyName));

        setLogger(null);

    }

    @Test
    public void propertiesMapperDoesNotCopyEmptyCollections() {

        // given
        final String expectedPropertyName = UUID.randomUUID().toString();
        final Supplier<List<String>> source = ArrayList::new;
        final Supplier<List<String>> target = () -> null;

        final Logger logger = mockTestLogger();

        // when
        ClientProviderPoliciesRegistry.PropertiesMapper.copyProperty(expectedPropertyName, source, target, value -> Assertions.fail("Should not copy"));

        //then
        final ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<String> propertyNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger).debug(logCaptor.capture(), any(), propertyNameCaptor.capture());

        assertThat(logCaptor.getValue(), containsString("source value is empty"));
        assertThat(propertyNameCaptor.getValue(), equalTo(expectedPropertyName));

        setLogger(null);

    }

    @Test
    public void propertiesMapperDoesNotCopyIfTargetStringIsNotBlank() {

        // given
        final String expectedPropertyName = UUID.randomUUID().toString();
        final Supplier<String> source = () -> UUID.randomUUID().toString();
        final Supplier<String> target = () -> UUID.randomUUID().toString();

        final Logger logger = mockTestLogger();

        // when
        ClientProviderPoliciesRegistry.PropertiesMapper.copyProperty(expectedPropertyName, source, target, value -> Assertions.fail("Should not copy"));

        //then
        final ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<String> propertyNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger).debug(logCaptor.capture(), any(), propertyNameCaptor.capture());

        assertThat(logCaptor.getValue(), containsString("target value is not empty"));
        assertThat(propertyNameCaptor.getValue(), equalTo(expectedPropertyName));

        setLogger(null);

    }

    @Test
    public void propertiesMapperDoesNotCopyIfTargetCollectionIsNotEmpty() {

        // given
        final String expectedPropertyName = UUID.randomUUID().toString();
        final Supplier<List<String>> source = () -> SplitUtil.split(TEST_SERVER_URIS);
        final Supplier<List<String>> target = () -> SplitUtil.split(TEST_SERVER_URIS + "1");

        final Logger logger = mockTestLogger();

        // when
        ClientProviderPoliciesRegistry.PropertiesMapper.copyProperty(expectedPropertyName, source, target, value -> Assertions.fail("Should not copy"));

        //then
        final ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<String> propertyNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger).debug(logCaptor.capture(), any(), propertyNameCaptor.capture());

        assertThat(logCaptor.getValue(), containsString("target value is not empty"));
        assertThat(propertyNameCaptor.getValue(), equalTo(expectedPropertyName));

        setLogger(null);

    }

}
