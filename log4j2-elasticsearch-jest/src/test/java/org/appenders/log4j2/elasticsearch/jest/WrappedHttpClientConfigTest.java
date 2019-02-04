package org.appenders.log4j2.elasticsearch.jest;

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
