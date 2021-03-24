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

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ResourceUtilTest {

    @Test
    public void loadsClasspathResourceIfResourceExists() {

        // when
        final String resource = ResourceUtil.loadResource("classpath:indexTemplate.json");

        // then
        assertNotNull(resource);

    }

    @Test
    public void loadsFileResourceIfResourceExists() {

        // when
        final String resource = ResourceUtil.loadResource(new File(ClassLoader.getSystemClassLoader().getResource("indexTemplate.json").getFile()).getAbsolutePath());

        // then
        assertNotNull(resource);

    }

    @Test
    public void throwsWhenUriIsNull() {

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ResourceUtil.loadResource(null));

        // then
        assertThat(exception.getMessage(), containsString("uri cannot be null"));

    }

    @Test
    public void throwsWhenClasspathResourceDoesNotExist() {

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ResourceUtil.loadResource("classpath:nonExistentFile"));

        // then
        assertThat(exception.getMessage(), containsString("Requested classpath resource was null"));

    }

    @Test
    public void throwsWhenFileDoesntExist() {

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ResourceUtil.loadResource("nonExistentFile"));

        // then
        assertThat(exception.getMessage(), containsString("Exception while loading file resource: nonExistentFile"));

    }

    @Test
    public void throwsOnInvalidProtocol() {

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> ResourceUtil.loadResource("~/nonExistentFile"));

        // then
        assertThat(exception.getMessage(), containsString("Exception while loading file resource: ~/nonExistentFile"));

    }

}
