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

import com.fasterxml.jackson.databind.ObjectWriter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.appenders.log4j2.elasticsearch.ByteBufBoundedSizeLimitPolicy;
import org.appenders.log4j2.elasticsearch.ByteBufPooledObjectOps;
import org.appenders.log4j2.elasticsearch.ExtendedPooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.ItemSourceFactory;
import org.appenders.log4j2.elasticsearch.JacksonJsonLayout;
import org.appenders.log4j2.elasticsearch.JacksonSerializer;
import org.appenders.log4j2.elasticsearch.Log4j2Lookup;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.ReusableOutputStreamProvider;
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

    private static final int ENVELOPE_SIZE = 1000;

    @Param(value = {
            "1",
            "10",
            "1000",
            "100000",
            "1000000",
    })
    public int poolSize;

    @Param(value = {
            "8192",
            "16384",
    })
    public int itemSizeInBytes;

    private PooledItemSourceFactory<LogEvent, ByteBuf> itemPool;
    private LogEventGenerator logEventGenerator;
    private JacksonSerializer<LogEvent> serializer;
    private int resizeCount;

    @Setup
    public void prepare() {

        final ExtendedPooledItemSourceFactory.Builder<Object, ByteBuf> builder = (ExtendedPooledItemSourceFactory.Builder<Object, ByteBuf>) new ExtendedPooledItemSourceFactory.Builder<Object, ByteBuf>()
                .withPooledObjectOps(new ByteBufPooledObjectOps(UnpooledByteBufAllocator.DEFAULT, new ByteBufBoundedSizeLimitPolicy(itemSizeInBytes, (itemSizeInBytes + ENVELOPE_SIZE) * 2)))
                .withInitialPoolSize(poolSize)
                .withPoolName("itemPool");

        this.itemPool = new ExtendedPooledItemSourceFactory<>(builder.configuredItemSourcePool(), new ReusableOutputStreamProvider<>());

        serializer = new JacksonSerializer<>(new JhmJacksonJsonLayout.Builder()
                .withSingleThread(true)
                .createConfiguredWriter());

        this.itemPool.start();

        logEventGenerator = ensureLogEventFitsBuffer();

    }

    private LogEventGenerator ensureLogEventFitsBuffer() {

        int envelopeSize = ENVELOPE_SIZE;
        int retries = 10;
        while (retries-- > 0) {
            final int messageSize = itemSizeInBytes / 2 - envelopeSize;
            final LogEventGenerator logEventGenerator = new LogEventGenerator(messageSize);
            final LogEvent next = logEventGenerator.next();
            final ItemSource<ByteBuf> itemSource = itemPool.create(next, serializer);
            try {
                if (itemSource.getSource().writerIndex() < itemSizeInBytes) {
                    System.out.println("Envelope size: " + envelopeSize);
                    System.out.println("Message size: " + messageSize);
                    System.out.println("Serialized event size: " + itemSource.getSource().writerIndex());
                    return logEventGenerator;
                }
                envelopeSize -= envelopeSize * 0.1;
            } finally {
                itemSource.release();
            }
        }
        throw new RuntimeException("Unable to create LogEventGenerator");
    }

    @Benchmark
    public void smokeTest(Blackhole fox) {
        final ItemSource<ByteBuf> itemSource = itemPool.create(logEventGenerator.next(), serializer);
        fox.consume(itemSource);
        itemSource.release();
    }

    private static class JhmJacksonJsonLayout extends JacksonJsonLayout {
        protected JhmJacksonJsonLayout(Configuration config, ObjectWriter configuredWriter, ItemSourceFactory<LogEvent, ByteBuf> itemSourceFactory) {
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
            public Builder withSingleThread(boolean singleThread) {
                super.withSingleThread(singleThread);
                return this;
            }

        }
    }

}
