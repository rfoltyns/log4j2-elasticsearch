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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.netty.buffer.UnpooledByteBufAllocator;
import net.openhft.affinity.AffinityLock;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.appenders.log4j2.elasticsearch.ByteBufBoundedSizeLimitPolicy;
import org.appenders.log4j2.elasticsearch.ByteBufPooledObjectOps;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.ItemSourceFactory;
import org.appenders.log4j2.elasticsearch.JacksonJsonLayout;
import org.appenders.log4j2.elasticsearch.Log4j2Lookup;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.ValueResolver;
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

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Fork(jvmArgsAppend = {
        "-ea",
        "-Xmx40g",
        "-Xms40g",
        "-XX:+AlwaysPreTouch",
        "-Djmh.pinned=true"
})
public class PooledItemSourceFactoryTest {

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

    private PooledItemSourceFactory itemPool;
    private ObjectWriter objectWriter;
    private byte[] bytes;

    private AffinityLock al;

    @Setup
    public void prepare() {

        this.itemPool = new PooledItemSourceFactory.Builder()
                .withPooledObjectOps(new ByteBufPooledObjectOps(UnpooledByteBufAllocator.DEFAULT, new ByteBufBoundedSizeLimitPolicy(itemSizeInBytes, itemSizeInBytes)))
                .withInitialPoolSize(poolSize)
                .withPoolName("itemPool")
                .build();

        this.objectWriter = new JhmJacksonJsonLayout.Builder()
                .withSingleThread(true)
                .createConfiguredWriter();

        this.bytes = new byte[itemSizeInBytes];

        new Random().nextBytes(bytes);

        itemPool.start();

    }

    @Benchmark
    public void smokeTest(Blackhole fox) {
        final ItemSource itemSource = itemPool.create(bytes, objectWriter);
        itemSource.release();
        fox.consume(itemSource);
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

}
