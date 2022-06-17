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

import org.appenders.log4j2.elasticsearch.ClientProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * Stores and retrieves {@link ClientProviderPolicy}-ies.
 */
public class ClientProviderPoliciesRegistry {

    private final Map<String, CopyingConfigPolicy<HttpClient>> copyingPolicies = new HashMap<>();

    public ClientProviderPoliciesRegistry() {
        register(new InheritServerUrisConfig());
        register(new InheritSecurityConfig());
    }

    /**
     * @return list of available policies
     */
    final List<String> availablePolicies() {
        final List<String> validPolicies = new ArrayList<>(Arrays.asList("shared", "none"));
        validPolicies.addAll(copyingPolicies.keySet());
        return validPolicies;
    }

    /**
     * Registers given {@link CopyingConfigPolicy}.
     *
     * @param policy policy to register
     */
    final void register(final CopyingConfigPolicy<HttpClient> policy) {
        copyingPolicies.put(policy.getName(), policy);
    }

    /**
     * Resolves {@link ClientProviderPolicy} from given policy list.
     *
     * By default, following policies are available:
     * <ul>
     *     <li><i>serverList</i> - copies uris config from applied {@link ClientProvider} to initial {@link ClientProvider}</li>
     *     <li><i>security</i> - copies security config from applied {@link ClientProvider} to initial {@link ClientProvider}</li>
     *     <li><i>shared</i> - reuses applied {@link ClientProvider} and ignores initial {@link ClientProvider}</li>
     *     <li><i>none</i> - ignores applied {@link ClientProvider} and uses initial {@link ClientProvider}</li>
     * </ul>
     *
     * <i>shared</i> and <i>none</i> can only be used exclusively, e.g. ["shared"] or ["none"]. If any other policy is present in the given list, exception will be thrown.
     *
     * <i>serverList</i> and <i>security</i> are {@link CopyingConfigPolicy}-ies and can be mixed with other {@link CopyingConfigPolicy}-ies, e.g.: ["serverList", "security", "myCustomCopyingPolicy"].
     *
     * @param policies policy names
     * @param initialClientProvider {@link ClientProviderPolicy} to apply the changes to
     * @return resolved {@link ClientProviderPolicy} if policies list was valid, throws otherwise
     *
     */
    public final ClientProviderPolicy<HttpClient> get(final Set<String> policies, final HttpClientProvider initialClientProvider) {

        validatePolicyList(policies);

        if (isValidExclusivePolicy(policies, "shared")) {
            return new SharedHttpClient();
        }

        if (isValidExclusivePolicy(policies, "none")) {
            return new NewHttpClient(initialClientProvider);
        }

        final CopyingConfigPolicyChain propertiesProcessor = new CopyingConfigPolicyChain(initialClientProvider);

        for (String policyName : policies) {
            propertiesProcessor.add(copyingPolicies.get(policyName));
        }

        return propertiesProcessor;

    }

    private void validatePolicyList(final Set<String> policies) {

        if (policies == null || policies.isEmpty()) {
            throw new IllegalArgumentException("Policy list must present. Valid policies: " + availablePolicies());
        }

        final List<String> availablePolicies = availablePolicies();
        for (String policyName: policies) {
            if (!availablePolicies.contains(policyName)) {
                throw new IllegalArgumentException("Invalid policy specified: [" + policyName + "]. Available policies: " + availablePolicies);
            }
        }

    }

    private boolean isValidExclusivePolicy(final Set<String> policies, final String shared) {
        if (policies.contains(shared)) {
            ensureNoOtherPolicies(policies, shared);
            return true;
        }
        return false;
    }

    private void ensureNoOtherPolicies(final Set<String> policies, final String policyName) {
        if (policies.size() > 1) {
            throw new IllegalArgumentException("Cannot apply other policies when [" + policyName + "] policy is used");
        }
    }

    /**
     * Allows to define mapping between two {@link ClientProvider} instances.
     *
     * @param <T> client type
     */
    public interface CopyingConfigPolicy<T> {

        /**
         * @return policy name to use on {@link #get(Set, HttpClientProvider)} calls.
         */
        String getName();

        /**
         * Copies properties from {@code source} to {@code target}.
         *
         * @param source source
         * @param target target
         */
        void copy(ClientProvider<T> source, ClientProvider<T> target);

    }

    /**
     * This API is highly experimental. Consider <i>private</i>.
     */
    static class PropertiesMapper {

        private PropertiesMapper() {
            // utility class
        }

        static <T> void copyProperty(final String propertyName, final Supplier<T> sourceValueProvider, final Supplier<T> targetValueProvider, final Consumer<T> valueConsumer) {

            if (!isEmpty(targetValueProvider.get())) {

                getLogger().debug("{}: Skipping [{}] as target value is not empty",
                        PropertiesMapper.class.getSimpleName(),
                        propertyName);
                return;

            }

            if (isEmpty(sourceValueProvider.get())) {

                getLogger().debug("{}: Skipping [{}] as source value is empty",
                        PropertiesMapper.class.getSimpleName(),
                        propertyName);
                return;

            }

            valueConsumer.accept(sourceValueProvider.get());

        }

        private static <T> boolean isEmpty(final T value) {

            if (value == null) {
                return true;
            }

            if (value instanceof String) {
                return ((String)value).trim().isEmpty();
            }

            if (value instanceof Collection) {
                return ((Collection)value).isEmpty();
            }

            return false;

        }

    }

    private static class CopyingConfigPolicyChain implements ClientProviderPolicy<HttpClient> {


        private final List<CopyingConfigPolicy<HttpClient>> policies = new ArrayList<>();
        private final ClientProvider<HttpClient> target;

        public CopyingConfigPolicyChain(final ClientProvider<HttpClient> target) {
            this.target = target;
        }

        void add(final CopyingConfigPolicy<HttpClient> policy) {
            this.policies.add(policy);
        }

        @Override
        public ClientProvider<HttpClient> apply(final ClientProvider<HttpClient> source) {

            process(source, target);

            getLogger().info("{}: Properties processed. Resolved config: {}",
                    CopyingConfigPolicyChain.class.getSimpleName(),
                    target);

            return target;

        }

        private void process(final ClientProvider<HttpClient> source, final ClientProvider<HttpClient> target) {
            for (CopyingConfigPolicy<HttpClient> policy : policies) {
                policy.copy(source, target);
            }
        }

    }

    private static class NewHttpClient implements ClientProviderPolicy<HttpClient> {

        private final HttpClientProvider target;

        private NewHttpClient(final HttpClientProvider httpClientProvider) {
            this.target = httpClientProvider;
        }

        @Override
        public ClientProvider<HttpClient> apply(final ClientProvider<HttpClient> source) {
            getLogger().info("{}: Parent config ignored. Resolved config: {}", NewHttpClient.class, target);
            return target;
        }

    }

    private static class SharedHttpClient implements ClientProviderPolicy<HttpClient> {

        @Override
        public ClientProvider<HttpClient> apply(final ClientProvider<HttpClient> source) {

            getLogger().info("{}: Parent config reused. Resolved config: {}",
                    SharedHttpClient.class.getSimpleName(),
                    source);

            return source;
        }

    }

    private static class InheritServerUrisConfig implements CopyingConfigPolicy<HttpClient> {

        @Override
        public String getName() {
            return "serverList";
        }

        @Override
        public void copy(final ClientProvider<HttpClient> source, final ClientProvider<HttpClient> target) {

            final HttpClientFactory.Builder src = ((HttpClientProvider)source).getHttpClientFactoryBuilder();
            final HttpClientFactory.Builder builder = ((HttpClientProvider)target).getHttpClientFactoryBuilder();

            PropertiesMapper.copyProperty("serverList", () -> src.serverList, () -> builder.serverList, builder::withServerList);

        }

    }

    private static class InheritSecurityConfig implements CopyingConfigPolicy<HttpClient> {

        @Override
        public String getName() {
            return "security";
        }

        @Override
        public void copy(final ClientProvider<HttpClient> source, final ClientProvider<HttpClient> target) {
            final HttpClientFactory.Builder src = ((HttpClientProvider)source).getHttpClientFactoryBuilder();

            final HttpClientFactory.Builder builder = ((HttpClientProvider)target).getHttpClientFactoryBuilder();

            PropertiesMapper.copyProperty("auth", () -> src.auth, () -> builder.auth, builder::withAuth);
            PropertiesMapper.copyProperty("realm", () -> src.realm, () -> builder.realm, builder::withRealm);
            PropertiesMapper.copyProperty("sslEngineFactory", () -> src.sslEngineFactory, () -> builder.sslEngineFactory, builder::withSslEngineFactory);

        }

    }

}
