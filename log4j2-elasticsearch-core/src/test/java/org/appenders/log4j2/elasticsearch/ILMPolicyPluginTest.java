package org.appenders.log4j2.elasticsearch;

import org.apache.logging.log4j.core.config.ConfigurationException;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ILMPolicyPluginTest {

    private static final String TEST_ILM_POLICY_NAME = "test-ilm-policy";
    private static final String TEST_PATH = "classpath:ilmPolicy.json";
    private static final String TEST_SOURCE = "{}";
    private static final String TEST_ROLLOVER_ALIAS = "test-rollover-alias";

    public static ILMPolicyPlugin.Builder createTestILMPolicyPluginBuilder() {
        ILMPolicyPlugin.Builder builder = ILMPolicyPlugin.newBuilder();
        builder.withName(TEST_ILM_POLICY_NAME)
                .withPath(TEST_PATH)
                .withRolloverAlias(TEST_ROLLOVER_ALIAS);
        return builder;
    }

    @Test
    public void startsWhenSetupCorrectlyWithNameAndPathAndRolloverAlias() {

        // given
        ILMPolicyPlugin.Builder builder = createTestILMPolicyPluginBuilder();
        builder.withName(TEST_ILM_POLICY_NAME)
                .withPath(TEST_PATH)
                .withRolloverAlias(TEST_ROLLOVER_ALIAS);

        // when
        ILMPolicyPlugin ilmPolicyPlugin = builder.build();

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
        ILMPolicyPlugin.Builder builder = createTestILMPolicyPluginBuilder();
        builder.withName(TEST_ILM_POLICY_NAME)
                .withRolloverAlias(TEST_ROLLOVER_ALIAS)
                .withPath(null)
                .withSource(TEST_SOURCE);

        // when
        ILMPolicyPlugin ilmPolicyPlugin = builder.build();

        // then
        assertNotNull(ilmPolicyPlugin);
        assertNotNull(ilmPolicyPlugin.getName());
        assertNotNull(ilmPolicyPlugin.getSource());
    }

    @Test(expected = ConfigurationException.class)
    public void builderThrowsExceptionWhenNameIsNotSet() {

        // given
        ILMPolicyPlugin.Builder builder = createTestILMPolicyPluginBuilder();
        builder.withName(null);

        // when
        builder.build();
    }

    @Test(expected = ConfigurationException.class)
    public void builderThrowsExceptionWhenRolloverAliasIsNotSet() {

        // given
        ILMPolicyPlugin.Builder builder = createTestILMPolicyPluginBuilder();
        builder.withRolloverAlias(null);

        // when
        builder.build();
    }

    @Test(expected = ConfigurationException.class)
    public void builderThrowsExceptionWhenNeitherPathOrSourceIsSet() {

        // given
        ILMPolicyPlugin.Builder builder = createTestILMPolicyPluginBuilder();
        builder.withPath(null)
                .withSource(null);

        // when
        builder.build();
    }

    @Test(expected = ConfigurationException.class)
    public void builderThrowsExceptionWhenBothPathAndSourceAreSet() {

        // given
        ILMPolicyPlugin.Builder builder = createTestILMPolicyPluginBuilder();
        builder.withPath(TEST_PATH)
                .withSource(TEST_SOURCE);

        // when
        builder.build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderThrowsExceptionWhenClasspathResourceDoesntExist() {

        // given
        ILMPolicyPlugin.Builder builder = createTestILMPolicyPluginBuilder();
        builder.withPath("classpath:nonExistentFile");

        // when
        builder.build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderThrowsExceptionWhenFileDoesNotExist() {

        // given
        ILMPolicyPlugin.Builder builder = createTestILMPolicyPluginBuilder();
        builder.withPath("nonExistentFile");

        // when
        builder.build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderThrowsExceptionOnInvalidProtocol() {

        // given
        ILMPolicyPlugin.Builder builder = createTestILMPolicyPluginBuilder();
        builder.withPath("~/nonExistentFile");

        // when
        builder.build();
    }

    @Test
    public void builderDoesNotThrowExceptionWhenFileExists() {

        // given
        ILMPolicyPlugin.Builder builder = createTestILMPolicyPluginBuilder();
        builder.withPath(new File(ClassLoader.getSystemClassLoader().getResource("ilmPolicy.json").getFile()).getAbsolutePath());

        // when
        builder.build();
    }

}
