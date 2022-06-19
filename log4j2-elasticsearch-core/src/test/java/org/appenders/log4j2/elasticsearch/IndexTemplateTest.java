package org.appenders.log4j2.elasticsearch;

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


import org.junit.jupiter.api.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IndexTemplateTest {

    public static final String TEST_INDEX_TEMPLATE = "testIndexTemplate";
    public static final String TEST_PATH = "classpath:indexTemplate.json";
    private static final String TEST_SOURCE = "{}";

    public static IndexTemplate.Builder createTestIndexTemplateBuilder() {
        IndexTemplate.Builder builder = new IndexTemplate.Builder();
        builder.withName(TEST_INDEX_TEMPLATE)
                .withPath(TEST_PATH);
        return builder;
    }

    @Test
    public void minimalConstructorSetsDefaultApiVersion() {

        // when
        final IndexTemplate indexTemplate = new IndexTemplate(TEST_INDEX_TEMPLATE, TEST_SOURCE);

        // then
        assertNotNull(indexTemplate);
        assertEquals(IndexTemplate.DEFAULT_API_VERSION, indexTemplate.getApiVersion());
        assertEquals(TEST_INDEX_TEMPLATE, indexTemplate.getName());
        assertNotNull(indexTemplate.getSource());
        assertEquals(IndexTemplate.TYPE_NAME, indexTemplate.getType());
    }

    @Test
    public void startsWhenSetupCorrectlyWithNonDefaultApiVersion() {

        // given
        final IndexTemplate.Builder builder = createTestIndexTemplateBuilder();
        builder.withName(TEST_INDEX_TEMPLATE)
                .withPath(TEST_PATH)
                .withApiVersion(7);

        // when
        final IndexTemplate indexTemplate = builder.build();

        // then
        assertNotNull(indexTemplate);
        assertNotEquals(IndexTemplate.DEFAULT_API_VERSION, indexTemplate.getApiVersion());
        assertEquals(7, indexTemplate.getApiVersion());
        assertEquals(TEST_INDEX_TEMPLATE, indexTemplate.getName());
        assertNotNull(indexTemplate.getSource());
        assertEquals(IndexTemplate.TYPE_NAME, indexTemplate.getType());
    }

    @Test
    public void startsWhenSetupCorrectlyWithNameAndPath() {

        // given
        final IndexTemplate.Builder builder = createTestIndexTemplateBuilder();
        builder.withName(TEST_INDEX_TEMPLATE)
                .withPath(TEST_PATH);

        // when
        final IndexTemplate indexTemplate = builder.build();

        // then
        assertNotNull(indexTemplate);
        assertEquals(IndexTemplate.DEFAULT_API_VERSION, indexTemplate.getApiVersion());
        assertEquals(TEST_INDEX_TEMPLATE, indexTemplate.getName());
        assertNotNull(indexTemplate.getSource());
        assertEquals(IndexTemplate.TYPE_NAME, indexTemplate.getType());
    }

    @Test
    public void startsWhenSetupCorrectlyWithNameAndSource() {

        // given
        final IndexTemplate.Builder builder = createTestIndexTemplateBuilder();
        builder.withName(TEST_INDEX_TEMPLATE)
                .withPath(null)
                .withSource(TEST_SOURCE);

        // when
        final IndexTemplate indexTemplate = builder.build();

        // then
        assertNotNull(indexTemplate);
        assertNotNull(indexTemplate.getName());
        assertNotNull(indexTemplate.getSource());
    }

    @Test
    public void builderThrowsWhenNameIsNotSet() {

        // given
        final IndexTemplate.Builder builder = createTestIndexTemplateBuilder();
        builder.withName(null);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), equalTo("No name provided for " + IndexTemplate.class.getSimpleName()));

    }

    @Test
    public void builderThrowsWhenNeitherPathOrSourceIsSet() {

        // given
        final IndexTemplate.Builder builder = createTestIndexTemplateBuilder();
        builder.withPath(null)
                .withSource(null);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), equalTo("Either path or source must to be provided for " + IndexTemplate.class.getSimpleName()));

    }

    @Test
    public void builderThrowsWhenBothPathAndSourceAreSet() {

        // given
        final IndexTemplate.Builder builder = createTestIndexTemplateBuilder();
        builder.withPath(TEST_PATH)
                .withSource(TEST_SOURCE);

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), equalTo("Either path or source must to be provided for " + IndexTemplate.class.getSimpleName()));

    }

    @Test
    public void builderThrowsWhenClasspathResourceDoesntExist() {

        // given
        final IndexTemplate.Builder builder = createTestIndexTemplateBuilder();
        builder.withPath("classpath:nonExistentFile");

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("Exception while loading classpath resource"));
        assertThat(exception.getMessage(), containsString("classpath:nonExistentFile"));

    }

    @Test
    public void builderThrowsWhenFileDoesNotExist() {

        // given
        final IndexTemplate.Builder builder = createTestIndexTemplateBuilder();
        builder.withPath("nonExistentFile");

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("Exception while loading file resource"));
        assertThat(exception.getMessage(), containsString("nonExistentFile"));


    }

    @Test
    public void builderThrowsOnInvalidProtocol() {

        // given
        final IndexTemplate.Builder builder = createTestIndexTemplateBuilder();
        builder.withPath("~/nonExistentFile");

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, builder::build);

        // then
        assertThat(exception.getMessage(), equalTo(
                "Exception while loading file resource: ~/nonExistentFile"));

    }

    @Test
    public void builderDoesntThrowWhenFileExists() {

        // given
        final IndexTemplate.Builder builder = createTestIndexTemplateBuilder();
        builder.withPath(new File(ClassLoader.getSystemClassLoader().getResource("indexTemplate.json").getFile()).getAbsolutePath());

        // when
        assertDoesNotThrow(builder::build);

    }

}
