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

public class IndexTemplatePluginTest {

    public static final String TEST_INDEX_TEMPLATE = "testIndexTemplate";
    public static final String TEST_PATH = "classpath:indexTemplate.json";
    private static final String TEST_SOURCE = "{}";

    private IndexTemplatePlugin createTestIndexTemplate(String name, String path) {
        return createTestIndexTemplate(name, path, null);
    }

    private IndexTemplatePlugin createTestIndexTemplate(String name, String path, String source) {
        return createTestIndexTemplate(IndexTemplate.DEFAULT_API_VERSION, name, path, source);
    }

    private IndexTemplatePlugin createTestIndexTemplate(int apiVersion, String name, String path, String source) {
        return IndexTemplatePlugin.createIndexTemplate(apiVersion, name, path, source);
    }

    @Test
    public void startsWhenSetupCorrectlyWithNameAndPath() {

        // when
        IndexTemplatePlugin indexTemplatePlugin = createTestIndexTemplate(TEST_INDEX_TEMPLATE, TEST_PATH);

        // then
        assertNotNull(indexTemplatePlugin);
        assertNotNull(indexTemplatePlugin.getName());
        assertNotNull(indexTemplatePlugin.getSource());
        assertEquals(IndexTemplatePlugin.TYPE_NAME, indexTemplatePlugin.getType());
    }

    @Test
    public void startsWhenSetupCorrectlyWithNameAndSource() {

        // when
        IndexTemplatePlugin indexTemplatePlugin= createTestIndexTemplate(TEST_INDEX_TEMPLATE, null, TEST_SOURCE);

        // then
        assertNotNull(indexTemplatePlugin);
        assertNotNull(indexTemplatePlugin.getName());
        assertNotNull(indexTemplatePlugin.getSource());
    }

    @Test
    public void setsDefaultApiVersionIfApiVersionIsZero() {

        // when
        IndexTemplatePlugin indexTemplatePlugin= createTestIndexTemplate(0, TEST_INDEX_TEMPLATE, null, TEST_SOURCE);

        // then
        assertNotNull(indexTemplatePlugin);
        assertEquals(IndexTemplate.DEFAULT_API_VERSION, indexTemplatePlugin.getApiVersion());
    }

    @Test
    public void setsApiVersionIfApiVersionIsNotZero() {

        // when
        IndexTemplatePlugin indexTemplatePlugin= createTestIndexTemplate(8, TEST_INDEX_TEMPLATE, null, TEST_SOURCE);

        // then
        assertNotNull(indexTemplatePlugin);
        assertEquals(8, indexTemplatePlugin.getApiVersion());
    }

    @Test
    public void minimalConstructorSetsDefaultApiVersion() {

        // when
        IndexTemplatePlugin indexTemplatePlugin = new IndexTemplatePlugin(TEST_INDEX_TEMPLATE, TEST_SOURCE);

        // then
        assertEquals(IndexTemplate.DEFAULT_API_VERSION, indexTemplatePlugin.getApiVersion());

    }

    @Test
    public void throwsByDefaultWhenNameIsNotSet() {

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, () -> createTestIndexTemplate(null, TEST_PATH));

        // then
        assertThat(exception.getMessage(), containsString("No name provided for " + IndexTemplate.class.getSimpleName()));

    }

    @Test
    public void throwsByDefaultWhenNeitherPathOrSourceIsSet() {

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, () -> createTestIndexTemplate(TEST_INDEX_TEMPLATE, null, null));

        // then
        assertThat(exception.getMessage(), containsString("Either path or source must to be provided for " + IndexTemplate.class.getSimpleName()));

    }

    @Test
    public void throwsByDefaultWhenBothPathAndSourceAreSet() {

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, () -> createTestIndexTemplate(TEST_INDEX_TEMPLATE, TEST_PATH, TEST_SOURCE));

        // then
        assertThat(exception.getMessage(), containsString("Either path or source must to be provided for " + IndexTemplate.class.getSimpleName()));

    }

    @Test
    public void throwsByDefaultWhenClasspathResourceDoesntExist() {

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, () -> createTestIndexTemplate(TEST_INDEX_TEMPLATE, "classpath:nonExistentFile"));

        // then
        assertThat(exception.getMessage(), containsString("classpath:nonExistentFile"));

    }

    @Test
    public void throwsByDefaultWhenFileDoesNotExist() {

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, () -> createTestIndexTemplate(TEST_INDEX_TEMPLATE, "nonExistentFile"));

        // then
        assertThat(exception.getMessage(), containsString("nonExistentFile"));

    }

    @Test
    public void throwsByDefaultOnInvalidProtocol() {

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, () -> createTestIndexTemplate(TEST_INDEX_TEMPLATE, "~/nonExistentFile"));

        // then
        assertThat(exception.getMessage(), containsString("~/nonExistentFile"));

    }

    @Test
    public void builderDoesntThrowExceptionWhenFileExists() {

        // given
        String existingFile = new File(ClassLoader.getSystemClassLoader()
                .getResource("indexTemplate.json").getFile())
                .getAbsolutePath();

        // when
        assertDoesNotThrow(() -> createTestIndexTemplate(TEST_INDEX_TEMPLATE, existingFile));

    }

}
