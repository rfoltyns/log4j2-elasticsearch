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


import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.File;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
        assertNotNull(indexTemplate);
        assertNotNull(indexTemplate.getName());
        assertNotNull(indexTemplate.getSource());
        assertEquals(IndexTemplate.TYPE_NAME, indexTemplate.getType());
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
        assertNotNull(indexTemplate);
        assertNotNull(indexTemplate.getName());
        assertNotNull(indexTemplate.getSource());
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

    @Test(expected = IllegalArgumentException.class)
    public void builderThrowsExceptionWhenClasspathResourceDoesntExist() {

        // given
        IndexTemplate.Builder builder = createTestIndexTemplateBuilder();
        builder.withPath("classpath:nonExistentFile");

        // when
        builder.build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void builderThrowsExceptionWhenFileDoesNotExist() {

        // given
        IndexTemplate.Builder builder = createTestIndexTemplateBuilder();
        builder.withPath("nonExistentFile");

        // when
        builder.build();
    }

    @Test(expected = IllegalArgumentException.class)
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

    @Test
    public void builderResolvesSourceWithValueResolverIfProvided() {

        // given
        String var1Name = UUID.randomUUID().toString();
        String var1Value = UUID.randomUUID().toString();
        String var2Name = UUID.randomUUID().toString();
        String var2Value = UUID.randomUUID().toString();

        System.setProperty(var1Name, var1Value);
        System.setProperty(var2Name, var2Value);

        String expected = String.format("{\n\t\"%s: \"%s\",\n\t\"%s: \"%s\"}",
                var1Name, var1Value, var2Name, var2Value);

        String source  = String.format("{\n\t\"%s: \"%s\",\n\t\"%s: \"%s\"}",
                var1Name, String.format("${sys:%s}", var1Name),
                var2Name, String.format("${sys:%s}", var2Name));

        IndexTemplate.Builder builder = createTestIndexTemplateBuilder()
                .withPath(null)
                .withSource(source)
                .withValueResolver(defaultTestValueResolver());

        // when
        IndexTemplate template = builder.build();

        // then
        assertEquals(expected, template.getSource());
    }

    @Test
    public void builderResolvesSourceIfValueResolverNotProvidedAndConfigurationProvided() {

        // given
        String var1Name = UUID.randomUUID().toString();
        String var1Value = UUID.randomUUID().toString();
        String var2Name = UUID.randomUUID().toString();
        String var2Value = UUID.randomUUID().toString();

        System.setProperty(var1Name, var1Value);
        System.setProperty(var2Name, var2Value);

        String expected = String.format("{\n\t\"%s: \"%s\",\n\t\"%s: \"%s\"}",
                var1Name, var1Value, var2Name, var2Value);

        String source  = String.format("{\n\t\"%s: \"%s\",\n\t\"%s: \"%s\"}",
                var1Name, String.format("${sys:%s}", var1Name),
                var2Name, String.format("${sys:%s}", var2Name));

        IndexTemplate.Builder builder = createTestIndexTemplateBuilder()
                .withPath(null)
                .withSource(source)
                .withConfiguration(LoggerContext.getContext(false).getConfiguration());

        // when
        IndexTemplate template = builder.build();

        // then
        assertEquals(expected, template.getSource());
    }

    @Test
    public void builderResolvesSourceWithNoopResolverIfValueResolverNotProvidedAndConfigurationNotProvided() {

        // given
        String var1Name = UUID.randomUUID().toString();
        String var1Value = UUID.randomUUID().toString();
        String var2Name = UUID.randomUUID().toString();
        String var2Value = UUID.randomUUID().toString();

        System.setProperty(var1Name, var1Value);
        System.setProperty(var2Name, var2Value);

        String source  = String.format("{\n\t\"%s: \"%s\",\n\t\"%s: \"%s\"}",
                var1Name, String.format("${sys:%s}", var1Name),
                var2Name, String.format("${sys:%s}", var2Name));

        IndexTemplate.Builder builder = createTestIndexTemplateBuilder()
                .withPath(null)
                .withSource(source)
                .withConfiguration(null)
                .withValueResolver(null);

        // when
        IndexTemplate template = builder.build();

        // then
        assertEquals(source, template.getSource());
    }

    @Test
    public void builderNoopResolverDoesntNotResolveVirtualProperty() {

        // given
        String var1Name = UUID.randomUUID().toString();
        String var1Value = UUID.randomUUID().toString();
        String var2Name = UUID.randomUUID().toString();
        String var2Value = UUID.randomUUID().toString();

        System.setProperty(var1Name, var1Value);
        System.setProperty(var2Name, var2Value);

        IndexTemplate.Builder builder = createTestIndexTemplateBuilder()
                .withPath(null)
                .withSource(TEST_SOURCE)
                .withConfiguration(null)
                .withValueResolver(null);

        ValueResolver resolver = builder.getValueResolver();

        VirtualProperty virtualProperty = new VirtualProperty(
                var1Name, String.format("${sys:%s}", var1Name), false);

        // when
        String result = resolver.resolve(virtualProperty);

        // then
        assertEquals(String.format("${sys:%s}", var1Name), result);
    }

    @NotNull
    public Log4j2Lookup defaultTestValueResolver() {
        return new Log4j2Lookup(new StrSubstitutor(new Interpolator()));
    }
}
