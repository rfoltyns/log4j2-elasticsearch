package org.appenders.log4j2.elasticsearch.hc;

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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.ObjectMessage;
import org.appenders.log4j2.elasticsearch.BatchBuilder;
import org.appenders.log4j2.elasticsearch.GenericItemSourceLayout;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.ItemSourceFactory;
import org.appenders.log4j2.elasticsearch.JacksonSerializer;
import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactoryTest;
import org.appenders.log4j2.elasticsearch.json.jackson.ExtendedLog4j2JsonModule;
import org.junit.jupiter.api.Test;

import java.util.Scanner;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public abstract class AbstractHCBatchOperationsTest {

    abstract public HCBatchOperations createDefaultBatchOperations(PooledItemSourceFactory itemSourceFactory);

    abstract public HCBatchOperations createDefaultBatchOperations(PooledItemSourceFactory itemSourceFactory, String mappingType);

    @Test
    public void throwsOnStringSource() {

        // given
        PooledItemSourceFactory itemSourceFactory = PooledItemSourceFactoryTest.createDefaultTestSourceFactoryConfig().build();
        HCBatchOperations batchOperations = createDefaultBatchOperations(itemSourceFactory, null);

        String indexName = UUID.randomUUID().toString();
        String source = UUID.randomUUID().toString();

        // when
        final UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> batchOperations.createBatchItem(indexName, source));

        // then
        assertThat(exception.getMessage(), equalTo("Use ItemSource based API instead"));

    }

    @Test
    public void createsBatchBuilder() {

        // given
        PooledItemSourceFactory itemSourceFactory = PooledItemSourceFactoryTest.createDefaultTestSourceFactoryConfig().build();
        BatchBuilder<BatchRequest> builder = createDefaultBatchOperations(itemSourceFactory, null).createBatchBuilder();

        // when
        BatchRequest request = builder.build();

        // then
        assertEquals(BatchRequest.class, request.getClass());
    }

    @Test
    public void throwsOnDeprecatedConfiguredWriter() {

        // given
        final PooledItemSourceFactory itemSourceFactory = PooledItemSourceFactoryTest.createDefaultTestSourceFactoryConfig().build();
        final HCBatchOperations batchOperations = createDefaultBatchOperations(itemSourceFactory, null);

        // when
        final UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, batchOperations::configuredWriter);

        // then
        assertThat(exception.getMessage(), containsString("Moved to ElasticsearchBulk"));

    }

    @Test
    public void defaultWriterCanSerializeBatchRequest() throws Exception {

        // given
        final PooledItemSourceFactory<Object, ByteBuf> itemSourceFactory = PooledItemSourceFactoryTest.createDefaultTestSourceFactoryConfig().build();

        final String expectedType = UUID.randomUUID().toString();
        final HCBatchOperations batchOperations = createDefaultBatchOperations(itemSourceFactory, expectedType);

        final GenericItemSourceLayout<Object, ByteBuf> layout = createDefaultTestLayoutBuilder(itemSourceFactory)
                .withSerializer(new JacksonSerializer.Builder<>()
                        .withJacksonModules(new ExtendedLog4j2JsonModule())
                        .build())
                .build();

        String expectedMessage = UUID.randomUUID().toString();
        long timeMillis = System.currentTimeMillis();
        Log4jLogEvent logEvent = Log4jLogEvent.newBuilder()
                .setTimeMillis(timeMillis)
                .setMessage(new ObjectMessage(expectedMessage)).build();

        final ItemSource itemSource = layout.serialize(logEvent);

        final String indexName = UUID.randomUUID().toString();
        final IndexRequest indexRequest = (IndexRequest) batchOperations.createBatchItem(indexName, itemSource);

        final BatchBuilder<BatchRequest> batchBuilder = batchOperations.createBatchBuilder();
        batchBuilder.add(indexRequest);

        // when
        final ByteBuf byteBuf = (ByteBuf) batchBuilder.build().serialize().getSource();

        // then
        final Scanner scanner = new Scanner(new ByteBufInputStream(byteBuf));

        final TestIndex deserializedAction = new ObjectMapper()
                .addMixIn(TestIndex.class, IndexRequestMixIn.class)
                .readValue(scanner.nextLine(), TestIndex.class);
        assertEquals(indexName, deserializedAction.index);
        assertEquals(expectedType, deserializedAction.type);

        final TestLogEvent deserializedDocument = new ObjectMapper().readValue(scanner.nextLine(), TestLogEvent.class);
        assertEquals(timeMillis, deserializedDocument.timeMillis);
        assertNotNull(deserializedDocument.level);
        assertNotNull(deserializedDocument.thread);
        assertEquals(expectedMessage, deserializedDocument.message);

    }

    @Test
    public void lifecycleStartStartItemSourceFactoryOnlyOnce() {

        // given
        PooledItemSourceFactory itemSourceFactory = mock(PooledItemSourceFactory.class);
        HCBatchOperations batchOperations = createDefaultBatchOperations(itemSourceFactory);

        // when
        batchOperations.start();
        batchOperations.start();

        // then
        verify(itemSourceFactory).start();

    }

    @Test
    public void lifecycleStopStopsItemSourceFactoryOnlyOnce() {

        // given
        PooledItemSourceFactory itemSourceFactory = mock(PooledItemSourceFactory.class);
        HCBatchOperations batchOperations = createDefaultBatchOperations(itemSourceFactory);

        batchOperations.start();

        // when
        batchOperations.stop();
        batchOperations.stop();

        // then
        verify(itemSourceFactory).stop();

    }

    @Test
    public void lifecycleStart() {

        // given
        LifeCycle lifeCycle = createLifeCycleTestObject();

        assertTrue(lifeCycle.isStopped());

        // when
        lifeCycle.start();

        // then
        assertFalse(lifeCycle.isStopped());
        assertTrue(lifeCycle.isStarted());

    }

    @Test
    public void lifecycleStop() {

        // given
        LifeCycle lifeCycle = createLifeCycleTestObject();

        assertTrue(lifeCycle.isStopped());

        lifeCycle.start();
        assertTrue(lifeCycle.isStarted());

        // when
        lifeCycle.stop();

        // then
        assertFalse(lifeCycle.isStarted());
        assertTrue(lifeCycle.isStopped());

    }

    private <T, R> GenericItemSourceLayout.Builder<T, R> createDefaultTestLayoutBuilder(final ItemSourceFactory<T, R> itemSourceFactory) {
        return new GenericItemSourceLayout.Builder<T, R>()
                .withItemSourceFactory(itemSourceFactory);
    }

    private LifeCycle createLifeCycleTestObject() {
        return createDefaultBatchOperations(mock(PooledItemSourceFactory.class), null);
    }

}
