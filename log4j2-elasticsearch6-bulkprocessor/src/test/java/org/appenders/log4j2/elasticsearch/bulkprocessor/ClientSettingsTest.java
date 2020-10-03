package org.appenders.log4j2.elasticsearch.bulkprocessor;

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

import org.elasticsearch.common.settings.Settings;
import org.junit.Test;

import static org.appenders.log4j2.elasticsearch.bulkprocessor.ClientSettingTest.TEST_NAME;
import static org.appenders.log4j2.elasticsearch.bulkprocessor.ClientSettingTest.TEST_VALUE;
import static org.appenders.log4j2.elasticsearch.bulkprocessor.ClientSettingTest.createDefaultTestClientSettingBuilder;
import static org.junit.Assert.assertEquals;

public class ClientSettingsTest {

    @Test
    public void builderBuildsSuccessfully() {

        // given
        ClientSetting clientSetting = createDefaultTestClientSettingBuilder()
                .withName(TEST_NAME)
                .withValue(TEST_VALUE).build();

        ClientSettings.Builder clientSettings = createDefaultTestClientSettingsBuilder()
                .withClientSettings(new ClientSetting[] { clientSetting });

        // when
        clientSettings.build();

    }

    @Test
    public void builderBuildsSuccessfullyWithNullSettings() {

        // given
        ClientSettings.Builder clientSettings = createDefaultTestClientSettingsBuilder()
                .withClientSettings(null);

        // when
        clientSettings.build();

    }

    @Test
    public void canApplySettings() {

        // given
        ClientSetting clientSetting = createDefaultTestClientSettingBuilder()
                .withName(TEST_NAME)
                .withValue(TEST_VALUE).build();

        ClientSettings.Builder clientSettings = createDefaultTestClientSettingsBuilder()
                .withClientSettings(new ClientSetting[] { clientSetting });

        ClientSettings settings = clientSettings.build();

        Settings.Builder builder = Settings.builder();

        // when
        settings.applyTo(builder);

        // then
        assertEquals(builder.get(TEST_NAME), TEST_VALUE);

    }

    private ClientSettings.Builder createDefaultTestClientSettingsBuilder() {
        return ClientSettings.newBuilder()
                .withClientSettings(new ClientSetting[] {
                        createDefaultTestClientSettingBuilder().build()
                });
    }

}
