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
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class IndexTemplatePluginTest {

    public static final String TEST_INDEX_TEMPLATE = "testIndexTemplate";
    public static final String TEST_PATH = "classpath:indexTemplate.json";
    private static final String TEST_SOURCE = "{}";

    private IndexTemplatePlugin createTestIndexTemplate(String name, String path) {
        return createTestIndexTemplate(name, path, null);
    }

    private IndexTemplatePlugin createTestIndexTemplate(String name, String path, String source) {
        return IndexTemplatePlugin.createIndexTemplate(name, path, source);
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

    @Test(expected = ConfigurationException.class)
    public void throwsExceptionByDefaultWhenNameIsNotSet() {

        // when
        createTestIndexTemplate(null, TEST_PATH);

    }

    @Test(expected = ConfigurationException.class)
    public void throwsExceptionByDefaultWhenNeitherPathOrSourceIsSet() {

        // when
        createTestIndexTemplate(TEST_INDEX_TEMPLATE, null, null);

    }

    @Test(expected = ConfigurationException.class)
    public void throwsExceptionByDefaultWhenBothPathAndSourceAreSet() {

        // when
        createTestIndexTemplate(TEST_INDEX_TEMPLATE, TEST_PATH, TEST_SOURCE);


    }

    @Test(expected = ConfigurationException.class)
    public void throwsExceptionByDefaultWhenClasspathResourceDoesntExist() {

        // when
        createTestIndexTemplate(null, "classpath:nonExistentFile");

    }

    @Test(expected = ConfigurationException.class)
    public void throwsExceptionByDefaultWhenFileDoesNotExist() {

        // when
        createTestIndexTemplate(null, "nonExistentFile");

    }

    @Test(expected = ConfigurationException.class)
    public void throwsExceptionByDefaultOnInvalidProtocol() {

        // when
        createTestIndexTemplate(null, "~/nonExistentFile");

    }

    @Test
    public void builderDoesntThrowExceptionWhenFileExists() {

        // given
        String existingFile = new File(ClassLoader.getSystemClassLoader()
                .getResource("indexTemplate.json").getFile())
                .getAbsolutePath();

        // when
        createTestIndexTemplate(TEST_INDEX_TEMPLATE, existingFile);

    }

}
