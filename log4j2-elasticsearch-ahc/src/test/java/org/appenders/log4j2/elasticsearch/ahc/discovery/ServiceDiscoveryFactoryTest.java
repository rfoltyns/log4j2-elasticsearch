package org.appenders.log4j2.elasticsearch.ahc.discovery;

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
import org.appenders.log4j2.elasticsearch.ahc.ClientProviderPolicy;
import org.appenders.log4j2.elasticsearch.ahc.HttpClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ServiceDiscoveryFactoryTest {

    @Test
    public void createsServiceDiscovery() {

        // given
        final ClientProviderPolicy<HttpClient> clientProviderPolicy = mock(ClientProviderPolicy.class);
        final ServiceDiscoveryRequest<HttpClient> serviceDiscoveryRequest = mock(ServiceDiscoveryRequest.class);
        final long refreshInterval = 250;

        final ServiceDiscoveryFactory<HttpClient> factory = new ServiceDiscoveryFactory<>(
                clientProviderPolicy,
                serviceDiscoveryRequest,
                refreshInterval);

        assertEquals(clientProviderPolicy, factory.clientProviderPolicy);
        assertEquals(serviceDiscoveryRequest, factory.serviceDiscoveryRequest);
        assertEquals(refreshInterval, factory.refreshInterval);

        final ClientProvider<HttpClient> clientProvider = mock(ClientProvider.class);

        // when
        final ServiceDiscovery serviceDiscovery = factory.create(clientProvider);

        // then
        assertNotNull(serviceDiscovery);
        verify(clientProviderPolicy).apply(same(clientProvider));

    }

}
