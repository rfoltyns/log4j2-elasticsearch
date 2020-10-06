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

public class ResourceUtilTest {

    @Test
    public void loadsClasspathResourceIfResourceExists() {

        // when
        ResourceUtil.loadResource("classpath:indexTemplate.json");

    }

    @Test
    public void loadsFileResourceIfResourceExists() {

        // when
        ResourceUtil.loadResource(new File(ClassLoader.getSystemClassLoader().getResource("indexTemplate.json").getFile()).getAbsolutePath());

    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsExceptionWhenUriIsNull() {

        // when
        ResourceUtil.loadResource(null);

    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsExceptionWhenClasspathResourceDoesNotExist() {

        // when
        ResourceUtil.loadResource("classpath:nonExistentFile");

    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsExceptionWhenFileDoesntExist() {

        // when
        ResourceUtil.loadResource("nonExistentFile");

    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsExceptionOnInvalidProtocol() {

        // when
        ResourceUtil.loadResource("~/nonExistentFile");

    }

}
