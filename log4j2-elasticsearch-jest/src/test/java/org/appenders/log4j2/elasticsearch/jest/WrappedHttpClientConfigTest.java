package org.appenders.log4j2.elasticsearch.jest;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2019 Rafal Foltynski
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

import io.searchbox.client.config.HttpClientConfig;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WrappedHttpClientConfigTest {

    @Test
    public void builderBuildsWithGivenHttpClientConfig() {

        // given
        HttpClientConfig givenConfig = new HttpClientConfig.Builder("http://localhost:9200").build();
        WrappedHttpClientConfig.Builder builder = new WrappedHttpClientConfig.Builder(givenConfig);

        // when
        WrappedHttpClientConfig config = builder.build();

        // then
        assertTrue(config.getHttpClientConfig() == givenConfig);

    }

    @Test
    public void builderBuildsWithGivenIoThreadCount() {

        // given
        HttpClientConfig givenConfig = new HttpClientConfig.Builder("http://localhost:9200").build();
        WrappedHttpClientConfig.Builder builder = new WrappedHttpClientConfig.Builder(givenConfig);

        int ioThreadCount = new Random().nextInt(1000) + 10;
        builder.ioThreadCount(ioThreadCount);

        // when
        WrappedHttpClientConfig config = builder.build();

        // then
        assertEquals(ioThreadCount, config.getIoThreadCount());

    }

}
