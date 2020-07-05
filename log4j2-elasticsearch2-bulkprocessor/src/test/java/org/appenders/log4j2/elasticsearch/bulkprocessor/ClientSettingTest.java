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
