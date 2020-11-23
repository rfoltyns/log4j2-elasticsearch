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

public class ComponentTemplatePluginTest {

    public static final String TEST_TEMPLATE_NAME = "testComponentTemplate";
    public static final String TEST_PATH = "classpath:componentTemplate.json";
    private static final String TEST_SOURCE = "{}";

    private ComponentTemplatePlugin createTestComponentTemplate(String name, String path) {
        return createTestComponentTemplate(name, path, null);
    }

    private ComponentTemplatePlugin createTestComponentTemplate(String name, String path, String source) {
        return ComponentTemplatePlugin.createComponentTemplate(name, path, source);
    }

    @Test
    public void startsWhenSetupCorrectlyWithNameAndPath() {

        // when
        ComponentTemplatePlugin plugin = createTestComponentTemplate(TEST_TEMPLATE_NAME, TEST_PATH);

        // then
        assertNotNull(plugin);
        assertNotNull(plugin.getName());
        assertNotNull(plugin.getSource());
        assertEquals(ComponentTemplatePlugin.TYPE_NAME, plugin.getType());
    }

    @Test
    public void startsWhenSetupCorrectlyWithNameAndSource() {

        // when
        ComponentTemplatePlugin plugin= createTestComponentTemplate(TEST_TEMPLATE_NAME, null, TEST_SOURCE);

        // then
        assertNotNull(plugin);
        assertNotNull(plugin.getName());
        assertNotNull(plugin.getSource());
    }

    @Test(expected = ConfigurationException.class)
    public void throwsExceptionByDefaultWhenNameIsNotSet() {

        // when
        createTestComponentTemplate(null, TEST_PATH);

    }

    @Test(expected = ConfigurationException.class)
    public void throwsExceptionByDefaultWhenNeitherPathOrSourceIsSet() {

        // when
        createTestComponentTemplate(TEST_TEMPLATE_NAME, null, null);

    }

    @Test(expected = ConfigurationException.class)
    public void throwsExceptionByDefaultWhenBothPathAndSourceAreSet() {

        // when
        createTestComponentTemplate(TEST_TEMPLATE_NAME, TEST_PATH, TEST_SOURCE);


    }

    @Test(expected = ConfigurationException.class)
    public void throwsExceptionByDefaultWhenClasspathResourceDoesntExist() {

        // when
        createTestComponentTemplate(null, "classpath:nonExistentFile");

    }

    @Test(expected = ConfigurationException.class)
    public void throwsExceptionByDefaultWhenFileDoesNotExist() {

        // when
        createTestComponentTemplate(null, "nonExistentFile");

    }

    @Test(expected = ConfigurationException.class)
    public void throwsExceptionByDefaultOnInvalidProtocol() {

        // when
        createTestComponentTemplate(null, "~/nonExistentFile");

    }

    @Test
    public void builderDoesntThrowExceptionWhenFileExists() {

        // given
        String existingFile = new File(ClassLoader.getSystemClassLoader()
                .getResource("componentTemplate.json").getFile())
                .getAbsolutePath();

        // when
        createTestComponentTemplate(TEST_TEMPLATE_NAME, existingFile);

    }

}
