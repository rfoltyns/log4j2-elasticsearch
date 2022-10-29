package org.appenders.log4j2.elasticsearch.ahc.jmh;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.appenders.log4j2.elasticsearch.AsyncBatchEmitter;
import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.appenders.log4j2.elasticsearch.BulkEmitter;
import org.appenders.log4j2.elasticsearch.ByteBufBoundedSizeLimitPolicy;
import org.appenders.log4j2.elasticsearch.ByteBufItemSource;
import org.appenders.log4j2.elasticsearch.ByteBufPooledObjectOps;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.ItemSourceFactory;
import org.appenders.log4j2.elasticsearch.JacksonJsonLayout;
import org.appenders.log4j2.elasticsearch.JacksonSerializer;
import org.appenders.log4j2.elasticsearch.Log4j2Lookup;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.ReleaseCallback;
import org.appenders.log4j2.elasticsearch.Serializer;
import org.appenders.log4j2.elasticsearch.UnlimitedResizePolicy;
import org.appenders.log4j2.elasticsearch.ValueResolver;
import org.appenders.log4j2.elasticsearch.ahc.AHCBatchOperations;
import org.appenders.log4j2.elasticsearch.ahc.BatchRequest;
import org.appenders.log4j2.elasticsearch.ahc.ElasticsearchBulkAPI;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static org.appenders.log4j2.elasticsearch.QueueFactory.getQueueFactoryInstance;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Fork(jvmArgsAppend = {
        "-ea",
        "-Xmx40g",
        "-Xms40g",
        "-XX:+AlwaysPreTouch",
        "-Djmh.pinned=true"
}
)
public class AsyncBatchEmitterWithBatchOperationsTest {

    public static final String INDEX_NAME = UUID.randomUUID().toString();

    private final ByteBufAllocator byteBufAllocator = UnpooledByteBufAllocator.DEFAULT;

    @Param({
            "1",
            "10",
            "1000",
            "10000",
    })
    public int itemPoolSize;

    @Param({
            "512",
            "1024",
            "2048",
            "4096",
            "8192",
            "16384",
    })
    public int itemSizeInBytes;

    private int batchSize;

    private AsyncBatchEmitter<BatchRequest> emitter;

    private final Queue<Object> itemsQueue = getQueueFactoryInstance(BulkEmitter.class.getSimpleName()).tryCreateMpscQueue(
            Integer.parseInt(System.getProperty("appenders." + BulkEmitter.class.getSimpleName() + ".initialSize", "65536")));
    private final Collection<Object> items = new ArrayList<>();

    private final AtomicLong bytesSerialized = new AtomicLong();

    @Setup
    public void prepare() {

        batchSize = itemPoolSize;

        final byte[] bytes = new byte[itemSizeInBytes];
        new Random().nextBytes(bytes);

        final PooledItemSourceFactory<byte[], ByteBuf> itemPool = new PooledItemSourceFactory.Builder<byte[], ByteBuf>()
                .withPooledObjectOps(new ByteBufPooledObjectOps(UnpooledByteBufAllocator.DEFAULT,
                        new ByteBufBoundedSizeLimitPolicy(itemSizeInBytes, itemSizeInBytes * 2)) {
                    @Override
                    public void reset(final ItemSource<ByteBuf> pooled) {
                        // don't remove test items content
                    }

                    @Override
                    public ByteBufItemSource createItemSource(final ReleaseCallback<ByteBuf> releaseCallback) {

                        final CompositeByteBuf buffer = new CompositeByteBuf(byteBufAllocator, true, 2);

                        return new ByteBufItemSource(buffer, releaseCallback) {
                            @Override
                            public void release() {
                                buffer.readerIndex(0); // simply prepare the item for the next test
                            }
                        };
                    }
                })
                .withInitialPoolSize(itemPoolSize)
                .withPoolName("itemPool")
                .build();

        final PooledItemSourceFactory<byte[], ByteBuf> batchPool = new PooledItemSourceFactory.Builder<byte[], ByteBuf>()
                .withPooledObjectOps(new ByteBufPooledObjectOps(UnpooledByteBufAllocator.DEFAULT,
                        new ByteBufBoundedSizeLimitPolicy(itemSizeInBytes * batchSize, itemSizeInBytes * batchSize * 2)))
                .withInitialPoolSize(1)
                .withMonitored(false)
                .withMonitorTaskInterval(1000)
                .withPoolName("batchPool")
                .withResizePolicy(new UnlimitedResizePolicy.Builder().withResizeFactor(0.5).build())
                .withResizeTimeout(100)
                .build();

        final BatchOperations<BatchRequest> batchOperations = new AHCBatchOperations(batchPool, new ElasticsearchBulkAPI());

        final Function<BatchRequest, Boolean> listener = batchRequest -> {

            try {
                @SuppressWarnings("unchecked")
                final ItemSource<ByteBuf> serialize = batchRequest.serialize();
                bytesSerialized.addAndGet(serialize.getSource().writerIndex());
                batchRequest.completed();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        };

        itemPool.start();

        final Serializer<byte[]> serializer = new JacksonSerializer<>(new JhmJacksonJsonLayout.Builder()
                .withSingleThread(true)
                .createConfiguredWriter());

        for (int i = 0; i < batchSize; i++) {
            final ItemSource<ByteBuf> itemSource = itemPool.create(bytes, serializer);
            items.add(batchOperations.createBatchItem(INDEX_NAME, itemSource));
        }

        emitter = new AsyncBatchEmitter<>(batchSize, 100000, batchOperations, itemsQueue);
        emitter.addListener(listener);

        batchPool.start();
        emitter.start();
    }

    @Benchmark
    public void smokeTest(final Blackhole fox) {
        itemsQueue.addAll(items);
        final boolean emit = emitter.emit(batchSize);
        fox.consume(emit);
    }

    private static class JhmJacksonJsonLayout extends JacksonJsonLayout {
        protected JhmJacksonJsonLayout(final Configuration config, final ObjectWriter configuredWriter, final ItemSourceFactory itemSourceFactory) {
            super(config, configuredWriter, itemSourceFactory);
        }

        static class Builder extends JacksonJsonLayout.Builder {

            @Override
            protected ValueResolver createValueResolver() {
                return new Log4j2Lookup(LoggerContext.getContext().getConfiguration().getStrSubstitutor());
            }

            @Override
            protected ObjectWriter createConfiguredWriter() {
                return super.createConfiguredWriter();
            }

            @Override
            protected ObjectMapper createDefaultObjectMapper() {
                return super.createDefaultObjectMapper();
            }

            @Override
            public Builder withSingleThread(final boolean singleThread) {
                super.withSingleThread(singleThread);
                return this;
            }

        }
    }

    @TearDown
    public void teardown() {
        System.out.println("Serialised batch bytes: " + bytesSerialized.getAndSet(0));
    }

}
