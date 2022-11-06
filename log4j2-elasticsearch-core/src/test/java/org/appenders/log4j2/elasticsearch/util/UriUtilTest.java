package org.appenders.log4j2.elasticsearch.util;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2022 Rafal Foltynski
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

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UriUtilTest {

    @Test
    public void appendsQueryParamIfQueryParamNotEmpty() {

        // given
        final String expectedQueryParam = UUID.randomUUID().toString();
        final StringBuilder sb = new StringBuilder();

        // when
        UriUtil.appendQueryParam(sb, "query_param", expectedQueryParam);
        final String result = sb.toString();

        // then
        assertThat(result, endsWith("query_param=" + expectedQueryParam));

    }

    @Test
    public void doesNotAppendQueryParamIfQueryParamEmpty() {

        // given
        final String expectedQueryParam = "";
        final StringBuilder sb = new StringBuilder();

        // when
        UriUtil.appendQueryParam(sb, "query_param", expectedQueryParam);
        final String result = sb.toString();

        // then
        assertTrue(result.isEmpty());

    }

    @Test
    public void doesNotAppendQueryParamIfQueryParamNull() {

        // given
        final String expectedQueryParam = null;
        final StringBuilder sb = new StringBuilder();

        // when
        UriUtil.appendQueryParam(sb, "query_param", expectedQueryParam);
        final String result = sb.toString();

        // then
        assertTrue(result.isEmpty());

    }

    @Test
    public void buildsValidQueryParamsWithMultipleParameters() {

        // given
        final String expectedQueryParam1 = UUID.randomUUID().toString();
        final String expectedQueryParam2 = UUID.randomUUID().toString();
        final String expectedQueryParam3 = UUID.randomUUID().toString();
        final StringBuilder sb = new StringBuilder();

        // when
        UriUtil.appendQueryParam(sb, "query_param1", expectedQueryParam1);
        UriUtil.appendQueryParam(sb, "query_param1", expectedQueryParam2);
        UriUtil.appendQueryParam(sb, "query_param2", expectedQueryParam3);
        final String result = sb.toString();

        // then
        assertEquals("?query_param1=" + expectedQueryParam1 + "&query_param1=" + expectedQueryParam2 + "&query_param2=" + expectedQueryParam3, result); // Yes, it's wrong. Will be improved in future releases.

    }

    @Test
    public void appendsPathIfPathNotEmpty() {

        // given
        final String expectedPath = UUID.randomUUID().toString();
        final StringBuilder sb = new StringBuilder();

        // when
        UriUtil.appendPath(sb, expectedPath);
        final String result = sb.toString();

        // then
        assertThat(result, startsWith("/" + expectedPath));

    }

    @Test
    public void doesNotAppendPathIfPathEmpty() {

        // given
        final String expectedPath = "";
        final StringBuilder sb = new StringBuilder();

        // when
        UriUtil.appendPath(sb, expectedPath);
        final String result = sb.toString();

        // then
        assertTrue(result.isEmpty());

    }

    @Test
    public void doesNotAppendPathIfPathNull() {

        // given
        final StringBuilder sb = new StringBuilder();

        // when
        UriUtil.appendPath(sb, null);
        final String result = sb.toString();

        // then
        assertTrue(result.isEmpty());

    }

    @Test
    public void buildsValidPathsWithPathAndMultipleParameters() {

        // given
        final String expectedPath1 = UUID.randomUUID().toString();
        final String expectedPath2 = UUID.randomUUID().toString();
        final String expectedPath3 = UUID.randomUUID().toString();
        final StringBuilder sb = new StringBuilder();

        // when
        UriUtil.appendPath(sb, expectedPath1);
        UriUtil.appendPath(sb, expectedPath2);
        sb.append("/"); // checking duplicate slashes
        UriUtil.appendPath(sb, expectedPath3);
        final String result = sb.toString();

        // then
        assertEquals("/" + expectedPath1 + "/" + expectedPath2 + "/" + expectedPath3, result);

    }

}
