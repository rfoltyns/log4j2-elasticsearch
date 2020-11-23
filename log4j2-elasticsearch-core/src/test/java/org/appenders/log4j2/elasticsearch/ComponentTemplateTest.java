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


import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ComponentTemplateTest {

    public static final String TEST_TEMPLATE_NAME = "testComponentTemplate";
    public static final String TEST_PATH = "classpath:componentTemplate.json";
    private static final String TEST_SOURCE = "{}";

    public static ComponentTemplate.Builder createTestComponentTemplateBuilder() {
        ComponentTemplate.Builder builder = new ComponentTemplate.Builder();
        builder.withName(TEST_TEMPLATE_NAME)
                .withPath(TEST_PATH);
        return builder;
    }

    @Test
    public void startsWhenSetupCorrectlyWithNameAndPath() {

        // given
        ComponentTemplate.Builder builder = createTestComponentTemplateBuilder();
        builder.withName(TEST_TEMPLATE_NAME)
                .withPath(TEST_PATH);

        // when
        ComponentTemplate template = builder.build();

        // then
        assertNotNull(template);
        assertNotNull(template.getName());
        assertNotNull(template.getSource());
        assertEquals(ComponentTemplate.TYPE_NAME, template.getType());
    }

    @Test
    public void startsWhenSetupCorrectlyWithNameAndSource() {

        // given
        ComponentTemplate.Builder builder = createTestComponentTemplateBuilder();
        builder.withName(TEST_TEMPLATE_NAME)
                .withPath(null)
                .withSource(TEST_SOURCE);

        // when
        ComponentTemplate template = builder.build();

        // then
        assertNotNull(template);
        assertNotNull(template.getName());
        assertNotNull(template.getSource());
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderThrowsExceptionWhenNameIsNotSet() {

        // given
        ComponentTemplate.Builder builder = createTestComponentTemplateBuilder();
        builder.withName(null);

        // when
        builder.build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderThrowsExceptionWhenNeitherPathOrSourceIsSet() {

        // given
        ComponentTemplate.Builder builder = createTestComponentTemplateBuilder();
        builder.withPath(null)
                .withSource(null);

        // when
        builder.build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderThrowsExceptionWhenBothPathAndSourceAreSet() {

        // given
        ComponentTemplate.Builder builder = createTestComponentTemplateBuilder();
        builder.withPath(TEST_PATH)
                .withSource(TEST_SOURCE);

        // when
        builder.build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderThrowsExceptionWhenClasspathResourceDoesntExist() {

        // given
        ComponentTemplate.Builder builder = createTestComponentTemplateBuilder();
        builder.withPath("classpath:nonExistentFile");

        // when
        builder.build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderThrowsExceptionWhenFileDoesNotExist() {

        // given
        ComponentTemplate.Builder builder = createTestComponentTemplateBuilder();
        builder.withPath("nonExistentFile");

        // when
        builder.build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderThrowsExceptionOnInvalidProtocol() {

        // given
        ComponentTemplate.Builder builder = createTestComponentTemplateBuilder();
        builder.withPath("~/nonExistentFile");

        // when
        builder.build();
    }

    @Test
    public void builderDoesntThrowExceptionWhenFileExists() {

        // given
        ComponentTemplate.Builder builder = createTestComponentTemplateBuilder();
        builder.withPath(new File(ClassLoader.getSystemClassLoader().getResource("componentTemplate.json").getFile()).getAbsolutePath());

        // when
        builder.build();
    }

}
