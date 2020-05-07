package org.appenders.log4j2.elasticsearch.bulkprocessor;

import org.elasticsearch.common.settings.Settings;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Test;

import static org.appenders.log4j2.elasticsearch.bulkprocessor.ClientSettingTest.TEST_NAME;
import static org.appenders.log4j2.elasticsearch.bulkprocessor.ClientSettingTest.TEST_VALUE;
import static org.appenders.log4j2.elasticsearch.bulkprocessor.ClientSettingTest.createDefaultTestClientSettingBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

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
