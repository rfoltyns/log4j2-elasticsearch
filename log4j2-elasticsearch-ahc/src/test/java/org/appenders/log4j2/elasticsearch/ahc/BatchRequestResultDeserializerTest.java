package org.appenders.log4j2.elasticsearch.ahc;

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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class BatchRequestResultDeserializerTest {

    private final Random random = new Random();

    @Test
    public void canDeserializeStatus() throws IOException {

        // given
        final int expectedStatus = random.nextInt(1000) + 1;
        final ObjectMapper mapper = new ObjectMapper()
                .addMixIn(BatchResult.class, BatchResultMixIn.class);
        final String json = String.format("{\"status\": %s}", expectedStatus);

        // when
        final BatchResult result = mapper.readerFor(BatchResult.class).readValue(json);

        // then
        assertEquals(expectedStatus, result.getStatusCode());

    }

    @Test
    public void canDeserializeTook() throws IOException {

        // given
        final int expectedTook = random.nextInt(1000) + 1;
        final ObjectMapper mapper = new ObjectMapper()
                .addMixIn(BatchResult.class, BatchResultMixIn.class);
        final String json = String.format("{\"took\": %s}", expectedTook);

        // when
        final BatchResult result = mapper.readerFor(BatchResult.class).readValue(json);

        // then
        assertEquals(expectedTook, result.getTook());

    }

    @Test
    public void canDeserializeErrors() throws IOException {

        // given
        final ObjectMapper mapper = new ObjectMapper()
                .addMixIn(BatchResult.class, BatchResultMixIn.class);
        final String json = "{\"errors\": true}";

        // when
        final BatchResult result = mapper.readerFor(BatchResult.class).readValue(json);

        // then
        assertFalse(result.isSucceeded());

    }

    @Test
    public void canDeserializeError() throws IOException {

        // given
        final ObjectMapper mapper = new ObjectMapper()
                .addMixIn(BatchResult.class, BatchResultMixIn.class);

        final int randomInt = this.random.nextInt(1000) + 1;
        final String expectedType = "type" + randomInt;
        final String expectedReason = "reason" + randomInt;

        final Error expectedCausedBy = new Error();
        expectedCausedBy.setType(expectedType);
        expectedCausedBy.setReason(expectedReason);

        final Error error = new Error();
        error.setType(expectedType);
        error.setReason(expectedReason);
        error.setCausedBy(expectedCausedBy);

        final BatchResult batchResult = new BatchResult(0, false, error, 0 ,null);
        final String json = mapper.writeValueAsString(batchResult);

        // when
        final BatchResult result = mapper.readerFor(BatchResult.class).readValue(json);

        // then
        assertEquals(error.getType(), result.getError().getType());
        assertEquals(error.getReason(), result.getError().getReason());
        assertEquals(error.getCausedBy().getType(), result.getError().getCausedBy().getType());
        assertEquals(error.getCausedBy().getReason(), result.getError().getCausedBy().getReason());

    }

    @Test
    public void canDeserializeItems() throws IOException {

        // given
        final ObjectMapper mapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .setVisibility(VisibilityChecker.Std.defaultInstance()
                        .withFieldVisibility(JsonAutoDetect.Visibility.ANY))
                .addMixIn(BatchResult.class, BatchResultMixIn.class);

        final String expectedId = UUID.randomUUID().toString();
        final String expectedIndex = UUID.randomUUID().toString();
        final String expectedType = UUID.randomUUID().toString();
        final int expectedStatus = random.nextInt(1000) + 1;

        final BatchItemResult item = new BatchItemResult();
        item.setId(expectedId);
        item.setIndex(expectedIndex);
        item.setType(expectedType);
        item.setStatus(expectedStatus);

        final List<BatchItemResult> items = new ArrayList<>();
        items.add(item);

        final BatchResult batchResult = new BatchResult(0, true, null, 0, items);
        final String json = mapper.writeValueAsString(batchResult);

        // when
        final BatchResult result = mapper.readerFor(BatchResult.class).readValue(json);

        // then
        assertEquals(1, result.getItems().size());
        final BatchItemResult resultItem = result.getItems().get(0);
        assertEquals(resultItem.getId(), expectedId);
        assertEquals(resultItem.getIndex(), expectedIndex);
        assertEquals(resultItem.getStatus(), expectedStatus);
        assertEquals(resultItem.getType(), expectedType);

    }

}
