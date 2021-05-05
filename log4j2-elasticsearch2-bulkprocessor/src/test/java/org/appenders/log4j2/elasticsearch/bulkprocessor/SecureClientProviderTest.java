package org.appenders.log4j2.elasticsearch.bulkprocessor;

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


import org.appenders.log4j2.elasticsearch.Auth;
import org.appenders.log4j2.elasticsearch.ClientProvider;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SecureClientProviderTest {

    @Test
    public void providedAuthIsUsedToCustomizeClient() {

        // given
        Auth<Settings.Builder> auth = mock(Auth.class);
        ClientProvider<TransportClient> clientProvider = new SecureClientProvider(auth);

        // when
        clientProvider.createClient();

        // then
        verify(auth).configure(any());

    }

    @Test
    public void providedClientSettingsAreUsedToCustomizeClient() {

        // given
        Auth<Settings.Builder> auth = mock(Auth.class);
        ClientSettings clientSettings = mock(ClientSettings.class);
        ClientProvider clientProvider = new SecureClientProvider(auth, clientSettings);

        // when
        clientProvider.createClient();

        // then
        verify(clientSettings).applyTo(any());
    }

}

