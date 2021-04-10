package org.appenders.log4j2.elasticsearch.hc;

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

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ByteBufResponseTest {

    @Test
    public void doesNotChangeErrorMessage() {

        // given
        String expectedMessage = UUID.randomUUID().toString();
        BasicResponse response = createDefaultTestObject()
                .withErrorMessage(expectedMessage);

        // when
        String errorMessage = response.getErrorMessage();

        // then
        assertEquals(expectedMessage, errorMessage);

    }

    @Test
    public void doesNotChangeResponseCode() {

        // given
        int expectedResponseCode = new Random().nextInt(1000) + 1;
        BasicResponse response = createDefaultTestObject()
                .withResponseCode(expectedResponseCode);

        // when
        int responseCode = response.getResponseCode();

        // then
        assertEquals(expectedResponseCode, responseCode);

    }

    @Test
    public void successfulIfResponseCodeBelow400() {

        // given
        BasicResponse response = createDefaultTestObject()
                .withResponseCode(new Random().nextInt(399) + 1);

        // then
        assertTrue(response.isSucceeded());

    }

    @Test
    public void nonSuccessfulIfResponseCodeEquals400() {

        // given
        BasicResponse response = createDefaultTestObject()
                .withResponseCode(400);

        // then
        assertFalse(response.isSucceeded());

    }

    @Test
    public void nonSuccessfulIfResponseCodeEqualsZero() {

        // given
        BasicResponse response = createDefaultTestObject()
                .withResponseCode(0);

        // then
        assertFalse(response.isSucceeded());

    }

    @Test
    public void nonSuccessfulIfResponseCodeAbove400() {

        // given
        BasicResponse response = createDefaultTestObject()
                .withResponseCode(new Random().nextInt(1000) + 400);

        // then
        assertFalse(response.isSucceeded());

    }

    private BasicResponse createDefaultTestObject() {
        return new BasicResponse()
                    .withResponseCode(500)
                    .withErrorMessage("test_error_message");
    }

}
