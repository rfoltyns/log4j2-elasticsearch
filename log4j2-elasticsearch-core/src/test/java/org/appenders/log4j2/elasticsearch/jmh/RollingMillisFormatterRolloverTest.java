package org.appenders.log4j2.elasticsearch.jmh;

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


import org.appenders.log4j2.elasticsearch.RollingMillisFormatter;
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

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Fork(jvmArgsAppend = {
        "-ea",
        "-Xmx2g",
        "-Xms2g",
        "-XX:+AlwaysPreTouch",
        "-Djmh.pinned=true"
})
public class RollingMillisFormatterRolloverTest {

    @Param({
            "32",
            "64",
            "128",
    })
    public int bufferSize;

    private RollingMillisFormatter formatter;

    @Setup
    public void prepare() {

        final byte[] bytes = new byte[bufferSize];
        new Random().nextBytes(bytes);

        final String pattern = "yyyy-MM-dd-HH-mm-ss-SSS";
        formatter = new RollingMillisFormatter.Builder()
                .withPattern(pattern)
                .withSeparator("-")
                .withPrefix(new String(bytes, StandardCharsets.UTF_8).substring(pattern.length() - 1))
                .build();

    }

    private final LogEventGenerator logEventGenerator = new LogEventGenerator(100, TimeUnit.MILLISECONDS.toMillis(1));

    @Benchmark
    public void smokeTest(final Blackhole fox) {
        final String indexName = formatter.format(logEventGenerator.next().getTimeMillis());
        fox.consume(indexName);
    }

}