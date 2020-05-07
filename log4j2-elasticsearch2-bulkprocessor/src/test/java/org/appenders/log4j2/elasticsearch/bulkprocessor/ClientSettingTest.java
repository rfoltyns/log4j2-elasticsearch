package org.appenders.log4j2.elasticsearch.bulkprocessor;

import org.apache.logging.log4j.core.config.ConfigurationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

public class ClientSettingTest {

    static final String TEST_NAME = "testClientSettingName";
    static final String TEST_VALUE = "testClientSettingValue";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void builderFailsWhenNameIsNull() {

        // given
        ClientSetting.Builder clientSetting = createDefaultTestClientSettingBuilder()
                .withName(null);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("No name provided for " + ClientSetting.NAME);

        // when
        clientSetting.build();

    }

    @Test
    public void builderFailsWhenValueIsNull() {

        // given
        ClientSetting.Builder clientSetting = createDefaultTestClientSettingBuilder()
                .withValue(null);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("No value provided for " + ClientSetting.NAME);

        // when
        clientSetting.build();

    }

    @Test
    public void builderBuildsSuccessfully() {

        // given
        ClientSetting.Builder clientSetting = createDefaultTestClientSettingBuilder()
                .withName(TEST_NAME)
                .withValue(TEST_VALUE);

        // when
        ClientSetting setting = clientSetting.build();

        // then
        assertEquals(TEST_NAME, setting.getName());
        assertEquals(TEST_VALUE, setting.getValue());

    }

    static ClientSetting.Builder createDefaultTestClientSettingBuilder() {
        return ClientSetting.newBuilder()
                .withName(TEST_NAME)
                .withValue(TEST_VALUE);
    }

}
