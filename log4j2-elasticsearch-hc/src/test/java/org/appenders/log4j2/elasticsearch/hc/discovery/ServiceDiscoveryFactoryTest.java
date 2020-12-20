package org.appenders.log4j2.elasticsearch.hc.discovery;

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

import org.appenders.log4j2.elasticsearch.ClientProvider;
import org.appenders.log4j2.elasticsearch.hc.ClientProviderPolicy;
import org.appenders.log4j2.elasticsearch.hc.HttpClient;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ServiceDiscoveryFactoryTest {

    @Test
    public void createsServiceDiscovery() {

        // given
        ClientProviderPolicy<HttpClient> clientProviderPolicy = mock(ClientProviderPolicy.class);
        ServiceDiscoveryRequest<HttpClient> serviceDiscoveryRequest = mock(ServiceDiscoveryRequest.class);
        long refreshInterval = 250;

        ServiceDiscoveryFactory<HttpClient> factory = new ServiceDiscoveryFactory<>(
                clientProviderPolicy,
                serviceDiscoveryRequest,
                refreshInterval);

        assertEquals(clientProviderPolicy, factory.clientProviderPolicy);
        assertEquals(serviceDiscoveryRequest, factory.serviceDiscoveryRequest);
        assertEquals(refreshInterval, factory.refreshInterval);

        ClientProvider<HttpClient> clientProvider = mock(ClientProvider.class);

        // when
        ServiceDiscovery serviceDiscovery = factory.create(clientProvider);

        // then
        assertNotNull(serviceDiscovery);
        verify(clientProviderPolicy).apply(same(clientProvider));

    }

}
