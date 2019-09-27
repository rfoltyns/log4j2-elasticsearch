package org.appenders.log4j2.elasticsearch.jest;

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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class BulkResultDeserializerTest {

    private Random random = new Random();

    @Test
    public void canDeserializeStatus() throws IOException {

        // given
        int expectedStatus = random.nextInt(1000) + 1;
        ObjectMapper mapper = new ObjectMapper()
                .addMixIn(BufferedBulkResult.class, BufferedBulkResultMixIn.class);
        String json = String.format("{\"status\": %s}", expectedStatus);

        // when
        BufferedBulkResult result = mapper.readerFor(BufferedBulkResult.class).readValue(json);

        // then
        assertEquals(expectedStatus, result.getStatus());

    }

    @Test
    public void canDeserializeTook() throws IOException {

        // given
        int expectedTook = random.nextInt(1000) + 1;
        ObjectMapper mapper = new ObjectMapper()
                .addMixIn(BufferedBulkResult.class, BufferedBulkResultMixIn.class);
        String json = String.format("{\"took\": %s}", expectedTook);

        // when
        BufferedBulkResult result = mapper.readerFor(BufferedBulkResult.class).readValue(json);

        // then
        assertEquals(expectedTook, result.getTook());

    }

    @Test
    public void canDeserializeErrors() throws IOException {

        // given
        ObjectMapper mapper = new ObjectMapper()
                .addMixIn(BufferedBulkResult.class, BufferedBulkResultMixIn.class);
        String json = String.format("{\"errors\": true}");

        // when
        BufferedBulkResult result = mapper.readerFor(BufferedBulkResult.class).readValue(json);

        // then
        assertFalse(result.isSucceeded());

    }

    @Test
    public void canDeserializeError() throws IOException {

        // given
        ObjectMapper mapper = new ObjectMapper()
                .addMixIn(BufferedBulkResult.class, BufferedBulkResultMixIn.class);

        int randomInt = this.random.nextInt(1000) + 1;
        String expectedType = "type" + randomInt;
        String expectedReason = "reason" + randomInt;

        BulkError expectedCausedBy = new BulkError();
        expectedCausedBy.setType(expectedType);
        expectedCausedBy.setReason(expectedReason);

        BulkError error = new BulkError();
        error.setType(expectedType);
        error.setReason(expectedReason);
        error.setCausedBy(expectedCausedBy);

        BufferedBulkResult bufferedBulkResult = new BufferedBulkResult(0, false, error, 0 ,null);
        String json = mapper.writeValueAsString(bufferedBulkResult);

        // when
        BufferedBulkResult result = mapper.readerFor(BufferedBulkResult.class).readValue(json);

        // then
        assertEquals(error.getType(), result.getError().getType());
        assertEquals(error.getReason(), result.getError().getReason());
        assertEquals(error.getCausedBy().getType(), result.getError().getCausedBy().getType());
        assertEquals(error.getCausedBy().getReason(), result.getError().getCausedBy().getReason());

    }

    @Test
    public void canDeserializeItems() throws IOException {

        // given
        ObjectMapper mapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .setVisibility(VisibilityChecker.Std.defaultInstance()
                        .withFieldVisibility(JsonAutoDetect.Visibility.ANY))
                .addMixIn(BufferedBulkResult.class, BufferedBulkResultMixIn.class);

        String expectedId = UUID.randomUUID().toString();
        String expectedIndex = UUID.randomUUID().toString();
        String expectedType = UUID.randomUUID().toString();
        int expectedStatus = random.nextInt(1000) + 1;

        BulkResultItem item = new BulkResultItem();
        item.setId(expectedId);
        item.setIndex(expectedIndex);
        item.setType(expectedType);
        item.setStatus(expectedStatus);

        List<BulkResultItem> items = new ArrayList<>();
        items.add(item);

        BufferedBulkResult bufferedBulkResult = new BufferedBulkResult(0, true, null, 0, items);
        String json = mapper.writeValueAsString(bufferedBulkResult);

        // when
        BufferedBulkResult result = mapper.readerFor(BufferedBulkResult.class).readValue(json);

        // then
        assertEquals(1, result.getItems().size());
        BulkResultItem resultItem = result.getItems().get(0);
        assertEquals(resultItem.getId(), expectedId);
        assertEquals(resultItem.getIndex(), expectedIndex);
        assertEquals(resultItem.getStatus(), expectedStatus);
        assertEquals(resultItem.getType(), expectedType);

    }

}
