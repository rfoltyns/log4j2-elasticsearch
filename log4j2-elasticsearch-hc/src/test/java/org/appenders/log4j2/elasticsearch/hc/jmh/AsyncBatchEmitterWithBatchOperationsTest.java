package org.appenders.log4j2.elasticsearch.hc.jmh;

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
import org.appenders.log4j2.elasticsearch.hc.BatchRequest;
import org.appenders.log4j2.elasticsearch.hc.HCBatchOperations;
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

import java.io.IOException;
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

    @Param(value = {
            "1",
            "10",
            "1000",
            "10000",
            "100000",
    })
    public int itemPoolSize;

    @Param(value = {
            "512",
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
                    public void reset(ItemSource<ByteBuf> pooled) {
                        // don't remove test items content
                    }

                    @Override
                    public ByteBufItemSource createItemSource(ReleaseCallback<ByteBuf> releaseCallback) {

                        CompositeByteBuf buffer = new CompositeByteBuf(byteBufAllocator, true, 2);

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
                .withResizePolicy(UnlimitedResizePolicy.newBuilder().withResizeFactor(0.5).build())
                .withResizeTimeout(100)
                .build();

        final BatchOperations<BatchRequest> batchOperations = new HCBatchOperations(batchPool);

        final Function<BatchRequest, Boolean> listener = batchRequest -> {

            try {
                final ItemSource<ByteBuf> serialize = batchRequest.serialize();
                bytesSerialized.addAndGet(serialize.getSource().writerIndex());
                batchRequest.completed();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        };

        itemPool.start();

        final Serializer<byte[]> serializer = new JacksonSerializer<>(new JhmJacksonJsonLayout.Builder()
                .withSingleThread(true)
                .createConfiguredWriter());

        for (int i = 0; i < batchSize; i++) {
            ItemSource<ByteBuf> itemSource = itemPool.create(bytes, serializer);
            items.add(batchOperations.createBatchItem(INDEX_NAME, itemSource));
        }

        emitter = new AsyncBatchEmitter<>(batchSize, 100000, batchOperations, itemsQueue);
        emitter.addListener(listener);

        batchPool.start();
        emitter.start();
    }

    @Benchmark
    public void smokeTest(Blackhole fox) {
        itemsQueue.addAll(items);
        final boolean emit = emitter.emit(batchSize);
        fox.consume(emit);
    }

    private static class JhmJacksonJsonLayout extends JacksonJsonLayout {
        protected JhmJacksonJsonLayout(Configuration config, ObjectWriter configuredWriter, ItemSourceFactory itemSourceFactory) {
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
            public Builder withSingleThread(boolean singleThread) {
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
