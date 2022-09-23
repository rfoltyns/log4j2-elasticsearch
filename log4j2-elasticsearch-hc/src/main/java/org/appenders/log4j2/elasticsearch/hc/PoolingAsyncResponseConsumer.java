package org.appenders.log4j2.elasticsearch.hc;

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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.ContentBufferEntity;
import org.apache.http.nio.protocol.AbstractAsyncResponseConsumer;
import org.apache.http.nio.util.SimpleInputBuffer;
import org.apache.http.protocol.HttpContext;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.ItemSourcePool;
import org.appenders.log4j2.elasticsearch.PoolResourceException;
import org.appenders.log4j2.elasticsearch.metrics.DefaultMetricsFactory;
import org.appenders.log4j2.elasticsearch.metrics.Metric;
import org.appenders.log4j2.elasticsearch.metrics.MetricConfig;
import org.appenders.log4j2.elasticsearch.metrics.MetricConfigFactory;
import org.appenders.log4j2.elasticsearch.metrics.Metrics;
import org.appenders.log4j2.elasticsearch.metrics.MetricsFactory;
import org.appenders.log4j2.elasticsearch.metrics.MetricsRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Uses provided {@link ItemSourcePool} to provide {@code org.apache.http.nio.util.SimpleInputBuffer}
 * instances for responses
 */
public class PoolingAsyncResponseConsumer extends AbstractAsyncResponseConsumer<HttpResponse> {

    private final ItemSourcePool<SimpleInputBuffer> itemSourcePool;

    private volatile HttpResponse response;
    private volatile ItemSource<SimpleInputBuffer> buffer;

    private final AsyncResponseConsumerMetrics metrics;

    public PoolingAsyncResponseConsumer(final AsyncResponseConsumerMetrics metrics, final ItemSourcePool<SimpleInputBuffer> pool) {
        this.itemSourcePool = pool;
        this.metrics = metrics;
    }

    public PoolingAsyncResponseConsumer(final ItemSourcePool<SimpleInputBuffer> bufferPool) {
        this.itemSourcePool = bufferPool;
        this.metrics = new AsyncResponseConsumerMetrics(PoolingAsyncResponseConsumer.class.getSimpleName(), new DefaultMetricsFactory(metricConfigs(false)));
    }

    @Override
    protected void onResponseReceived(final HttpResponse response) {
        this.response = response;
    }

    /**
     * Writes content of given {@code org.apache.http.HttpEntity} to pooled buffer
     *
     * @param entity response entity
     * @param contentType content type of given entity
     * @throws IOException on pool errors
     */
    @Override
    protected void onEntityEnclosed(
            final HttpEntity entity, final ContentType contentType) throws IOException {

        if (buffer == null) {

            buffer = getPooled();

            // SimpleInputBuffer passed just to satisfy the constructor
            ContentBufferEntity bufferedEntity = new ContentBufferEntity(entity, buffer.getSource());

            // override the content here (see ContentBufferEntity constructor)
            bufferedEntity.setContent(new ItemSourceContentInputStream(buffer));

            this.response.setEntity(bufferedEntity);

            metrics.recordContentLength((int) entity.getContentLength());

        }

    }

    @Override
    protected void onContentReceived(
            final ContentDecoder decoder, final IOControl ioctrl) throws IOException {
        this.buffer.getSource().consumeContent(decoder);
    }

    /**
     * Resets the state. Does NOT release underlying buffer
     */
    @Override
    protected void releaseResources() {
        this.response = null;
        this.buffer = null;
    }

    @Override
    protected HttpResponse buildResult(final HttpContext context) {
        return this.response;
    }

    ItemSource<SimpleInputBuffer> getPooled() throws IOException {
        try {
            return itemSourcePool.getPooled();
        } catch (PoolResourceException e) {
            throw new IOException("Unable get pooled response buffer: " + e.getMessage());
        }
    }

    public static List<MetricConfig> metricConfigs(final boolean enabled) {

        final List<MetricConfig> result = new ArrayList<>();
        result.add(MetricConfigFactory.createCountConfig(enabled, "responseBytes"));
        return result;

    }

    static final class AsyncResponseConsumerMetrics implements Metrics {

        private final List<MetricsRegistry.Registration> registrations = new ArrayList<>();;
        private final Metric responseContentLength;

        AsyncResponseConsumerMetrics(final String name, final MetricsFactory metricsFactory) {
            this.responseContentLength = metricsFactory.createMetric(name, "responseBytes");
        }

        final void recordContentLength(int contentLength) {
            responseContentLength.store(contentLength);
        }

        @Override
        public void register(final MetricsRegistry registry) {
            registrations.add(registry.register(responseContentLength));
        }

        @Override
        public void deregister() {
            registrations.forEach(MetricsRegistry.Registration::deregister);
            registrations.clear();
        }

    }

}
