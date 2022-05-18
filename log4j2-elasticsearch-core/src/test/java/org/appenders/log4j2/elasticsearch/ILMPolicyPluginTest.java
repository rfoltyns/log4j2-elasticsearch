package org.appenders.log4j2.elasticsearch;

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
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ILMPolicyPluginTest {

    private static final String TEST_ILM_POLICY_NAME = "test-ilm-policy";
    private static final String TEST_PATH = "classpath:ilmPolicy.json";
    private static final String TEST_SOURCE = "{}";
    private static final String TEST_ROLLOVER_ALIAS = "test-rollover-alias";

    public static ILMPolicyPlugin.Builder createTestILMPolicyPluginBuilder() {
        final ILMPolicyPlugin.Builder builder = ILMPolicyPlugin.newBuilder();
        builder.withName(TEST_ILM_POLICY_NAME)
                .withPath(TEST_PATH)
                .withRolloverAlias(TEST_ROLLOVER_ALIAS);
        return builder;
    }

    @Test
    public void startsWhenSetupCorrectlyWithNameAndPathAndRolloverAlias() {

        // given
        final ILMPolicyPlugin.Builder builder = createTestILMPolicyPluginBuilder();
        builder.withName(TEST_ILM_POLICY_NAME)
                .withPath(TEST_PATH)
                .withRolloverAlias(TEST_ROLLOVER_ALIAS);

        // when
        final ILMPolicyPlugin ilmPolicyPlugin = builder.build();

        // then
        assertNotNull(ilmPolicyPlugin);
        assertNotNull(ilmPolicyPlugin.getName());
        assertNotNull(ilmPolicyPlugin.getSource());
        assertEquals(ILMPolicy.TYPE_NAME, ilmPolicyPlugin.getType());
        assertEquals(TEST_ROLLOVER_ALIAS, ilmPolicyPlugin.getRolloverAlias());
    }

    @Test
    public void startsWhenSetupCorrectlyWithNameAndSourceAndRolloverAlias() {

        // given
        final ILMPolicyPlugin.Builder builder = createTestILMPolicyPluginBuilder();
        builder.withName(TEST_ILM_POLICY_NAME)
                .withRolloverAlias(TEST_ROLLOVER_ALIAS)
                .withPath(null)
                .withSource(TEST_SOURCE);

        // when
        final ILMPolicyPlugin ilmPolicyPlugin = builder.build();

        // then
        assertNotNull(ilmPolicyPlugin);
        assertNotNull(ilmPolicyPlugin.getName());
        assertNotNull(ilmPolicyPlugin.getSource());
    }

    @Test
    public void builderthrowsWhenNameIsNotSet() {

        // given
        final ILMPolicyPlugin.Builder builder = createTestILMPolicyPluginBuilder();
        builder.withName(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString(
                "No name provided for " + ILMPolicyPlugin.PLUGIN_NAME));

    }

    @Test
    public void builderthrowsWhenRolloverAliasIsNotSet() {

        // given
        final ILMPolicyPlugin.Builder builder = createTestILMPolicyPluginBuilder();
        builder.withRolloverAlias(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString(
                "No rolloverAlias provided for " + ILMPolicyPlugin.PLUGIN_NAME));

    }

    @Test
    public void builderthrowsWhenNeitherPathOrSourceIsSet() {

        // given
        final ILMPolicyPlugin.Builder builder = createTestILMPolicyPluginBuilder();
        builder.withPath(null)
                .withSource(null);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString(
                "Either path or source must to be provided for " + ILMPolicyPlugin.PLUGIN_NAME));

    }

    @Test
    public void builderthrowsWhenBothPathAndSourceAreSet() {

        // given
        final ILMPolicyPlugin.Builder builder = createTestILMPolicyPluginBuilder();
        builder.withPath(TEST_PATH)
                .withSource(TEST_SOURCE);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString(
                "Either path or source must to be provided for " + ILMPolicyPlugin.PLUGIN_NAME));

    }

    @Test
    public void builderthrowsWhenClasspathResourceDoesntExist() {

        // given
        final ILMPolicyPlugin.Builder builder = createTestILMPolicyPluginBuilder();
        builder.withPath("classpath:nonExistentFile");

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString(
                "classpath:nonExistentFile"));

    }

    @Test
    public void builderthrowsWhenFileDoesNotExist() {

        // given
        final ILMPolicyPlugin.Builder builder = createTestILMPolicyPluginBuilder();
        builder.withPath("nonExistentFile");

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString(
                "nonExistentFile"));

    }

    @Test
    public void builderthrowsOnInvalidProtocol() {

        // given
        final ILMPolicyPlugin.Builder builder = createTestILMPolicyPluginBuilder();
        builder.withPath("~/nonExistentFile");

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString(
                "~/nonExistentFile"));

    }

    @Test
    public void builderDoesNotThrowExceptionWhenFileExists() {

        // given
        final ILMPolicyPlugin.Builder builder = createTestILMPolicyPluginBuilder();
        builder.withPath(new File(ClassLoader.getSystemClassLoader().getResource("ilmPolicy.json").getFile()).getAbsolutePath());

        // when
        assertDoesNotThrow(builder::build);

    }

}
