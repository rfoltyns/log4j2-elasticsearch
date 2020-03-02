package org.appenders.log4j2.elasticsearch.failover;

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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import net.openhft.chronicle.map.ChronicleMap;
import org.appenders.log4j2.elasticsearch.ByteBufItemSource;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static java.lang.Math.abs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MarshallingTest {

    static {
        System.setProperty("io.netty.allocator.maxOrder", "1");
    }

    private Random random = new Random();

    @Test
    public void canMarshallKeySequenceConfig() throws IOException {

        // given
        ChronicleMap<CharSequence, ItemSource> map = createDefaultTestChronicleMap();

        // when
        long expectedSeqId = randomLong();
        long expectedOwnerId = randomLong();
        long expectedReaderIndex = randomLong();
        long expectedWriterIndex = randomLong();

        KeySequenceConfig config = new KeySequenceConfig(expectedSeqId, expectedReaderIndex, expectedWriterIndex);
        config.setOwnerId(expectedOwnerId);

        CharSequence expectedId = config.getKey();

        // when
        map.put(config.getKey(), config);

        // then
        KeySequenceConfig result = (KeySequenceConfig) map.get(config.getKey());

        assertEquals(expectedId, result.getKey());
        assertEquals(expectedOwnerId, result.getOwnerId());
        assertEquals(expectedSeqId, result.getSeqId());
        assertEquals(expectedReaderIndex, result.nextReaderIndex());
        assertEquals(expectedWriterIndex, result.nextWriterIndex());

    }

    @Test
    public void canMarshallKeySequenceConfigList() throws IOException {

        // given
        ChronicleMap<CharSequence, ItemSource> map = createDefaultTestChronicleMap();
        List<CharSequence> keys = new ArrayList<>();

        String key1 = UUID.randomUUID().toString();
        String key2 = UUID.randomUUID().toString();
        keys.add(key1);
        keys.add(key2);

        KeySequenceConfigKeys expected = new KeySequenceConfigKeys(keys);
        String expectedKey = UUID.randomUUID().toString();

        // when
        map.put(expectedKey, expected);
        KeySequenceConfigKeys result = (KeySequenceConfigKeys) map.get(expectedKey);

        // then
        Collection<CharSequence> source = result.getSource().getKeys();
        assertTrue(source.contains(key1));
        assertTrue(source.contains(key2));

    }

    @Test
    public void canMarshallFailedItemSource() throws IOException {

        // given
        CompositeByteBuf byteBuf = UnpooledByteBufAllocator.DEFAULT.compositeHeapBuffer(2);
        byte[] bytes = new byte[512];
        random.nextBytes(bytes);

        String expectedPayload = new String(bytes, Charset.defaultCharset());

        byteBuf.writeBytes(bytes);

        assertEquals(expectedPayload, byteBuf.toString(Charset.defaultCharset()));

        ItemSource<ByteBuf> expectedSource = new ByteBufItemSource(byteBuf, (source) -> byteBuf.release());

        String targetName = UUID.randomUUID().toString();
        FailedItemSource<ByteBuf> failedItemSource = new FailedItemSource<>(
                expectedSource,
                new FailedItemInfo(targetName)
        );

        ChronicleMap<CharSequence, ItemSource> map = createDefaultTestChronicleMap();
        String key = UUID.randomUUID().toString();

        // when
        map.put(key, failedItemSource);
        FailedItemSource<ByteBuf> result = (FailedItemSource<ByteBuf>) map.get(key);

        // then
        assertEquals(expectedPayload, result.getSource().toString(Charset.defaultCharset()));
        assertEquals(failedItemSource.getInfo().getTargetName(), result.getInfo().getTargetName());

    }

    private ChronicleMap<CharSequence, ItemSource> createDefaultTestChronicleMap() throws IOException {
        File file = createTempFile();
        return new ChronicleMapRetryFailoverPolicy.Builder()
                .withFileName(file.getAbsolutePath())
                .withNumberOfEntries(10)
                .withAverageValueSize(1024)
                .withBatchSize(1)
                .createChronicleMap();
    }

    private File createTempFile() {
        try {
            return File.createTempFile("test", ".chronicleMap");
        } catch (IOException e) {
            Assert.fail("Cannot create temp file");
            return null;
        }
    }

    private long randomLong() {
        return abs(random.nextLong());
    }

}
