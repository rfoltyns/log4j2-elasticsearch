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


import org.apache.logging.log4j.core.config.ConfigurationException;
import org.elasticsearch.common.settings.Settings;
import org.junit.jupiter.api.Test;

import static org.appenders.log4j2.elasticsearch.bulkprocessor.BasicCredentials.SHIELD_SECURITY_USER;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BasicCredentialsTest {

    private static final String TEST_USER = "test_user";
    private static final String TEST_PASSWORD = "changeme";

    public static BasicCredentials.Builder createTestBuilder() {
        return BasicCredentials.newBuilder()
                .withUsername(TEST_USER)
                .withPassword(TEST_PASSWORD);
    }

    @Test
    public void minimalBuilderTest() {

        // given
        BasicCredentials.Builder builder = createTestBuilder();

        // when
        BasicCredentials certInfo = builder.build();

        // then
        assertNotNull(certInfo);

    }

    @Test
    public void throwsWhenUsernameIsNull() {

        // given
        BasicCredentials.Builder builder = createTestBuilder()
                .withUsername(null);

        // when
        ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("username"));

    }

    @Test
    public void throwsWhenPasswordIsNull() {

        // given
        BasicCredentials.Builder builder = createTestBuilder()
                .withPassword(null);

        // when
        ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("password"));

    }

    @Test
    public void objectIsConfiguredWhenAllParamsAreSet() {

        // given
        BasicCredentials basicCredentials = createTestBuilder()
                .withUsername(TEST_USER)
                .withPassword(TEST_PASSWORD)
                .build();

        Settings.Builder settings = Settings.builder();

        // when
        basicCredentials.applyTo(settings);

        // then
        assertEquals(TEST_USER + ":" + TEST_PASSWORD, settings.get(SHIELD_SECURITY_USER));

    }

}
