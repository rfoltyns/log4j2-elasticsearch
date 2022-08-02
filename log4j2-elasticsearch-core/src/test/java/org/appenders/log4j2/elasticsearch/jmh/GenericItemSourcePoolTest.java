package org.appenders.log4j2.elasticsearch.jmh;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2021 Rafal Foltynski
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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.appenders.log4j2.elasticsearch.ByteBufBoundedSizeLimitPolicy;
import org.appenders.log4j2.elasticsearch.ByteBufPooledObjectOps;
import org.appenders.log4j2.elasticsearch.ExtendedPooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.ItemSourcePool;
import org.appenders.log4j2.elasticsearch.PoolResourceException;
import org.appenders.log4j2.elasticsearch.UnlimitedResizePolicy;
import org.appenders.log4j2.elasticsearch.metrics.MetricConfigFactory;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Collections;
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


    @Param({
//            "1024",
//            "2048",
            "4096",
            "8192",
//            "100000",
//            "1000000",
    })
    public int poolSize;

    @Param({
            "512",
//            "2048",
//            "8192",
//            "16384",
    })
    public int itemSizeInBytes;

    private ItemSourcePool<ByteBuf> itemPool;

    @Setup
    public void prepare() {

        final ExtendedPooledItemSourceFactory.Builder itemPoolBuilder = (ExtendedPooledItemSourceFactory.Builder) new ExtendedPooledItemSourceFactory.Builder()
                .withPoolName("itemPool")
                .withInitialPoolSize(poolSize)
                .withNullOnEmptyPool(true)
                .withResizePolicy(new UnlimitedResizePolicy.Builder().build())
                .withMetricConfigs(Collections.singletonList(MetricConfigFactory.createMaxConfig(true, "available", false)))
                .withPooledObjectOps(new ByteBufPooledObjectOps(UnpooledByteBufAllocator.DEFAULT, new ByteBufBoundedSizeLimitPolicy(itemSizeInBytes, itemSizeInBytes)));

        this.itemPool = itemPoolBuilder.configuredItemSourcePool();

        itemPool.start();

    }

    @Benchmark
    public void smokeTest(Blackhole fox) throws PoolResourceException {
        final ItemSource itemSource = itemPool.getPooledOrNull();
        itemSource.release();
        fox.consume(itemSource);
    }

}
