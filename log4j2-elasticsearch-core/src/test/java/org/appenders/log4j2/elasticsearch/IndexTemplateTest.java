package org.appenders.log4j2.elasticsearch;

import org.apache.logging.log4j.core.config.ConfigurationException;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class IndexTemplateTest {

    public static final String TEST_INDEX_TEMPLATE = "testIndexTemplate";
    public static final String TEST_PATH = "classpath:indexTemplate.json";
    private static final String TEST_SOURCE = "{}";

    public static IndexTemplate.Builder createTestIndexTemplateBuilder() {
        IndexTemplate.Builder builder = IndexTemplate.newBuilder();
        builder.withName(TEST_INDEX_TEMPLATE)
                .withPath(TEST_PATH);
        return builder;
    }

    @Test
    public void startsWhenSetupCorrectlyWithNameAndPath() {

        // given
        IndexTemplate.Builder builder = createTestIndexTemplateBuilder();
        builder.withName(TEST_INDEX_TEMPLATE)
                .withPath(TEST_PATH);

        // when
        IndexTemplate indexTemplate = builder.build();

        // then
        Assert.assertNotNull(indexTemplate);
        Assert.assertNotNull(indexTemplate.getName());
        Assert.assertNotNull(indexTemplate.getSource());
    }

    @Test
    public void startsWhenSetupCorrectlyWithNameAndSource() {

        // given
        IndexTemplate.Builder builder = createTestIndexTemplateBuilder();
        builder.withName(TEST_INDEX_TEMPLATE)
                .withPath(null)
                .withSource(TEST_SOURCE);

        // when
        IndexTemplate indexTemplate = builder.build();

        // then
        Assert.assertNotNull(indexTemplate);
        Assert.assertNotNull(indexTemplate.getName());
        Assert.assertNotNull(indexTemplate.getSource());
    }

    @Test(expected = ConfigurationException.class)
    public void builderThrowsExceptionWhenNameIsNotSet() {

        // given
        IndexTemplate.Builder builder = createTestIndexTemplateBuilder();
        builder.withName(null);

        // when
        builder.build();
    }

    @Test(expected = ConfigurationException.class)
    public void builderThrowsExceptionWhenNeitherPathOrSourceIsSet() {

        // given
        IndexTemplate.Builder builder = createTestIndexTemplateBuilder();
        builder.withPath(null)
                .withSource(null);

        // when
        builder.build();
    }

    @Test(expected = ConfigurationException.class)
    public void builderThrowsExceptionWhenBothPathAndSourceAreSet() {

        // given
        IndexTemplate.Builder builder = createTestIndexTemplateBuilder();
        builder.withPath(TEST_PATH)
                .withSource(TEST_SOURCE);

        // when
        builder.build();
    }

    @Test(expected = ConfigurationException.class)
    public void builderThrowsExceptionWhenClasspathResourceDoesntExist() {

        // given
        IndexTemplate.Builder builder = createTestIndexTemplateBuilder();
        builder.withPath("classpath:nonExistentFile");

        // when
        builder.build();
    }

    @Test(expected = ConfigurationException.class)
    public void builderThrowsExceptionWhenFileDoesntExist() {

        // given
        IndexTemplate.Builder builder = createTestIndexTemplateBuilder();
        builder.withPath("nonExistentFile");

        // when
        builder.build();
    }

    @Test(expected = ConfigurationException.class)
    public void builderThrowsExceptionOnInvalidProtocol() {

        // given
        IndexTemplate.Builder builder = createTestIndexTemplateBuilder();
        builder.withPath("~/nonExistentFile");

        // when
        builder.build();
    }

    @Test
    public void builderDoesntThrowExceptionWhenFileExists() {

        // given
        IndexTemplate.Builder builder = createTestIndexTemplateBuilder();
        builder.withPath(new File(ClassLoader.getSystemClassLoader().getResource("indexTemplate.json").getFile()).getAbsolutePath());

        // when
        builder.build();
    }

}
