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

import org.appenders.log4j2.elasticsearch.BulkEmitter;
import org.appenders.log4j2.elasticsearch.QueueFactory;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Queue;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Fork(jvmArgsAppend = {
        "-ea",
        "-Xmx2g",
        "-Xms2g",
        "-XX:+AlwaysPreTouch",
        "-Djmh.pinned=true",
}
)
public class QueueFactoryTest {

    static final int SIZE = 10000;

    private QueueFactory queueFactory;

    @Setup
    public void prepare() {
        this.queueFactory = QueueFactory.getQueueFactoryInstance(BulkEmitter.class.getSimpleName());
    }

    @Benchmark
    public void createMpMcQueue(Blackhole fox) {
        final Queue<Object> queue = queueFactory.tryCreateMpmcQueue(SIZE);
        fox.consume(queue);
    }

    @Benchmark
    public void createMpScQueue(Blackhole fox) {
        final Queue<Object> queue = queueFactory.tryCreateMpscQueue(SIZE);
        fox.consume(queue);
    }

    @Benchmark
    public void createSpScQueue(Blackhole fox) {
        final Queue<Object> queue = queueFactory.tryCreateSpscQueue(SIZE);
        fox.consume(queue);
    }

}
