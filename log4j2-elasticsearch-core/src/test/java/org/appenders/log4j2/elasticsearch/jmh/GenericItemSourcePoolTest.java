package org.appenders.log4j2.elasticsearch.jmh;

import io.netty.buffer.ByteBuf;
import net.openhft.affinity.AffinityLock;
import org.appenders.log4j2.elasticsearch.ExtendedPooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.ItemSourcePool;
import org.appenders.log4j2.elasticsearch.PoolResourceException;
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

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Fork(jvmArgsAppend = {
        "-ea",
        "-Xmx40g",
        "-Xms40g",
        "-XX:+AlwaysPreTouch",
        "-Djmh.pinned=true",
}
)
public class GenericItemSourcePoolTest {


    @Param(value = {
            "1",
            "10",
            "1000",
            "100000",
            "1000000",
    })
    public int poolSize;

    @Param(value = {
            "512",
            "2048",
            "8192",
            "16384",
    })
    public int itemSizeInBytes;

    private ItemSourcePool<ByteBuf> itemPool;

    @Setup
    public void prepare() {

        final ExtendedPooledItemSourceFactory.Builder itemPoolBuilder = (ExtendedPooledItemSourceFactory.Builder) new ExtendedPooledItemSourceFactory.Builder()
                .withPoolName("itemPool")
                .withInitialPoolSize(poolSize)
                .withItemSizeInBytes(itemSizeInBytes);

        this.itemPool = itemPoolBuilder.configuredItemSourcePool();

        itemPool.start();

    }

    @Benchmark
    public void smokeTest(Blackhole fox) throws PoolResourceException {
        final ItemSource itemSource = itemPool.getPooled();
        itemSource.release();
        fox.consume(itemSource);
    }

}
