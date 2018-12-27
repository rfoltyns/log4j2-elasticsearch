package org.appenders.log4j2.elasticsearch.jest;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2018 Rafal Foltynski
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
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.searchbox.core.Bulk;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.ObjectMessage;
import org.appenders.log4j2.elasticsearch.BatchBuilder;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.JacksonJsonLayout;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactoryTest;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.Scanner;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.spy;

public class BufferedBulkOperationsTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void throwsOnStringSource() {

        // given
        PooledItemSourceFactory bufferedSourceFactory = PooledItemSourceFactoryTest.createDefaultTestSourceFactoryConfig().build();
        BufferedBulkOperations bufferedBulkOperations = new BufferedBulkOperations(bufferedSourceFactory);

        String indexName = UUID.randomUUID().toString();
        String source = UUID.randomUUID().toString();

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("Use ItemSource based API instead");

        // when
        bufferedBulkOperations.createBatchItem(indexName, source);

    }

    @Test
    public void createsBufferedBulkBuilder() {

        // given
        PooledItemSourceFactory bufferedSourceFactory = PooledItemSourceFactoryTest.createDefaultTestSourceFactoryConfig().build();
        BatchBuilder<Bulk> builder = new BufferedBulkOperations(bufferedSourceFactory).createBatchBuilder();

        // when
        Bulk bulk = builder.build();

        // then
        assertEquals(BufferedBulk.class, bulk.getClass());
    }

    @Test
    public void createsConfiguredWriter() {

        // given
        PooledItemSourceFactory bufferedSourceFactory = PooledItemSourceFactoryTest.createDefaultTestSourceFactoryConfig().build();
        BufferedBulkOperations bufferedBulkOperations = new BufferedBulkOperations(bufferedSourceFactory);

        // when
        ObjectWriter writer = bufferedBulkOperations.configuredWriter();

        // then
        Assert.assertNotNull(writer);

    }

    @Test
    public void createsConfiguredReader() {

        // given
        PooledItemSourceFactory bufferedSourceFactory = PooledItemSourceFactoryTest.createDefaultTestSourceFactoryConfig().build();
        BufferedBulkOperations bufferedBulkOperations = new BufferedBulkOperations(bufferedSourceFactory);

        // when
        ObjectReader reader = bufferedBulkOperations.configuredReader();

        // then
        Assert.assertNotNull(reader);

    }

    @Test
    public void defaultWriterCanSerializeBufferedBulk() throws IOException {

        // given
        PooledItemSourceFactory bufferedSourceFactory = PooledItemSourceFactoryTest.createDefaultTestSourceFactoryConfig().build();

        BufferedBulkOperations bufferedBulkOperations = new BufferedBulkOperations(bufferedSourceFactory);

        JacksonJsonLayout layout = createDefaultTestJacksonJsonLayout(bufferedSourceFactory);

        String expectedMessage = UUID.randomUUID().toString();
        long timeMillis = System.currentTimeMillis();
        Log4jLogEvent logEvent = Log4jLogEvent.newBuilder()
                .setTimeMillis(timeMillis)
                .setMessage(new ObjectMessage(expectedMessage)).build();

        ItemSource itemSource = layout.toSerializable(logEvent);

        String indexName = UUID.randomUUID().toString();
        BufferedIndex bufferedIndex = (BufferedIndex) bufferedBulkOperations.createBatchItem(indexName, itemSource);

        BatchBuilder<Bulk> batchBuilder = bufferedBulkOperations.createBatchBuilder();
        batchBuilder.add(bufferedIndex);

        // when
        ByteBuf byteBuf = ((BufferedBulk)batchBuilder.build()).serializeRequest();

        // then
        Scanner scanner = new Scanner(new ByteBufInputStream(byteBuf));

        TestIndex deserializedAction = new ObjectMapper()
                .addMixIn(TestIndex.class, BulkableActionMixIn.class)
                .readValue(scanner.nextLine(), TestIndex.class);
        assertEquals(indexName, deserializedAction.index);
        assertNotNull(deserializedAction.type);

        TestLogEvent deserializedDocument = new ObjectMapper().readValue(scanner.nextLine(), TestLogEvent.class);
        assertEquals(timeMillis, deserializedDocument.timeMillis);
        assertNotNull(deserializedDocument.level);
        assertNotNull(deserializedDocument.thread);
        assertEquals(expectedMessage, deserializedDocument.message);

    }

    private JacksonJsonLayout createDefaultTestJacksonJsonLayout(PooledItemSourceFactory bufferedSourceFactory) {
        JacksonJsonLayout.Builder builder = spy(JacksonJsonLayout.newBuilder());
        builder.withItemSourceFactory(bufferedSourceFactory);
        return builder.build();
    }

}
