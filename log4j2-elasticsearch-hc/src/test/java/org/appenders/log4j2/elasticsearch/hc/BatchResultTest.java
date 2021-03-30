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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BatchResultTest {

    private static final String DEFAULT_TEST_MESSAGE = "default_test_message";
    private static final int DEFAULT_TEST_STATUS = new Random().nextInt(1000) + 1;

    @Test
    public void isSucceededReturnsFalseWhenErrorsIsTrueAndErrorIsNull() {

        // given
        BatchResult result = createTestBatchResult(true, null);

        // when
        boolean succeeded = result.isSucceeded();

        // then
        assertFalse(succeeded);

    }

    @Test
    public void isSucceededReturnsFalseWhenErrorsIsFalseAndErrorIsNotNull() {

        // given
        BatchResult result = createTestBatchResult(false, new Error());

        // when
        boolean succeeded = result.isSucceeded();

        // then
        assertFalse(succeeded);

    }

    @Test
    public void isSucceededReturnsFalseWhenErrorsIsTrueAndErrorIsNotNull() {

        // given
        BatchResult result = createTestBatchResult(true, new Error());

        // when
        boolean succeeded = result.isSucceeded();

        // then
        assertFalse(succeeded);

    }

    @Test
    public void isSucceededReturnsTrueWhenErrorsIsFalseAndErrorIsNull() {

        // given
        BatchResult result = createTestBatchResult(false, null);

        // when
        boolean succeeded = result.isSucceeded();

        // then
        assertTrue(succeeded);

    }

    @Test
    public void errorMessageDefaultsToProvidedIfRequestIsSucceeeded() {

        // given
        String expectedMessage = UUID.randomUUID().toString();

        BatchResult result = createTestBatchResult(false, null);
        result.withErrorMessage(expectedMessage);

        // when
        String actualMessage = result.getErrorMessage();

        // then
        assertEquals(expectedMessage, actualMessage);

    }

    @Test
    public void errorMessageContainsStatusIfAvailableAndNotSucceeded() {

        // given
        String expectedMessage = UUID.randomUUID().toString();

        BatchResult result = createTestBatchResult(false, new Error());
        result.withErrorMessage(expectedMessage);

        // when
        String actualMessage = result.getErrorMessage();

        // then
        assertTrue(actualMessage.contains("" + DEFAULT_TEST_STATUS));

    }

    @Test
    public void errorMessageDoesNotContainStatusIfAvailableAndNotHigherThanZeroAndNotSucceeded() {

        // given
        String expectedMessage = "test_error_message";

        BatchResult result = new BatchResult(0, false, new Error(), 0, null);
        result.withErrorMessage(expectedMessage);

        // when
        String actualMessage = result.getErrorMessage();

        // then
        assertFalse(actualMessage.contains("" + 0));

    }

    @Test
    public void errorMessageDoesNotContainStatusIfHigherThanZeroAndSucceeded() {

        // given
        String expectedMessage = "error_message";

        BatchResult result = new BatchResult(0, false, null, 1, null);
        result.withErrorMessage(expectedMessage);

        // when
        String actualMessage = result.getErrorMessage();

        // then
        assertFalse(actualMessage.contains("" + 1));

    }

    @Test
    public void errorMessageDoesNotContainStatusIfStatusEqualsZeroAndSucceeded() {

        // given
        String expectedMessage = "error_message";

        BatchResult result = new BatchResult(0, false, null, 0, null);
        result.withErrorMessage(expectedMessage);

        // when
        String actualMessage = result.getErrorMessage();

        // then
        assertFalse(actualMessage.contains("" + 0));

    }

    @Test
    public void errorMessageContainsRootErrorInfoIfAvailable() {

        // given
        String expectedType = UUID.randomUUID().toString();
        String expectedReason = UUID.randomUUID().toString();

        Error error = new Error();
        error.setType(expectedType);
        error.setReason(expectedReason);

        BatchResult result = createTestBatchResult(false, error);
        result.withErrorMessage(DEFAULT_TEST_MESSAGE);

        // when
        String actualMessage = result.getErrorMessage();

        // then
        assertTrue(actualMessage.contains(expectedType));
        assertTrue(actualMessage.contains(expectedReason));

    }

    @Test
    public void errorMessageContainsRootErrorCausedByInfoIfAvailable() {

        // given
        String expectedType = UUID.randomUUID().toString();
        String expectedReason = UUID.randomUUID().toString();

        String expectedCausedByType = UUID.randomUUID().toString();
        String expectedCausedByReason = UUID.randomUUID().toString();

        Error causedByError = new Error();
        causedByError.setType(expectedCausedByType);
        causedByError.setReason(expectedCausedByReason);

        Error error = new Error();
        error.setType(expectedType);
        error.setReason(expectedReason);
        error.setCausedBy(causedByError);

        BatchResult result = createTestBatchResult(false, error);
        result.withErrorMessage(DEFAULT_TEST_MESSAGE);

        // when
        String actualMessage = result.getErrorMessage();

        // then
        assertTrue(actualMessage.contains(expectedCausedByType));
        assertTrue(actualMessage.contains(expectedCausedByReason));
        assertFalse(actualMessage.contains(expectedType));
        assertFalse(actualMessage.contains(expectedReason));


    }

    @Test
    public void errorMessageContainsInfoWhenItemsListIsNullAndErrorsIsTrue() {

        // given
        String expectedMessage = BatchResult.UNABLE_TO_GET_MORE_INFO;

        BatchResult result = createTestBatchResult(true, null, null);
        result.withErrorMessage(expectedMessage);

        // when
        String actualMessage = result.getErrorMessage();

        // then
        assertTrue(actualMessage.contains(expectedMessage));

    }

    @Test
    public void errorMessageContainsInfoWhenItemsListIsEmptyAndErrorsIsTrue() {

        // given
        String expectedMessage = BatchResult.UNABLE_TO_GET_MORE_INFO;

        BatchResult result = createTestBatchResult(true, null, new ArrayList<>());
        result.withErrorMessage(expectedMessage);

        // when
        String actualMessage = result.getErrorMessage();

        // then
        assertTrue(actualMessage.contains(expectedMessage));

    }

    @Test
    public void errorMessageContainsFailedItemErrorInfoIfAvailableAndErrorsIsTrue() {

        // given
        String expectedType = UUID.randomUUID().toString();
        String expectedReason = UUID.randomUUID().toString();

        Error error = new Error();
        error.setType(expectedType);
        error.setReason(expectedReason);

        BatchItemResult successfulItem = new BatchItemResult();
        BatchItemResult failed = new BatchItemResult();
        failed.setError(error);

        List<BatchItemResult> items = new ArrayList<>();
        items.add(successfulItem);
        items.add(failed);

        BatchResult result = createTestBatchResult(true, null, items);
        result.withErrorMessage(DEFAULT_TEST_MESSAGE);

        // when
        String actualMessage = result.getErrorMessage();

        // then
        assertTrue(actualMessage.contains(expectedType));
        assertTrue(actualMessage.contains(expectedReason));

    }

    @Test
    public void errorMessageDoesNotContainFailedItemErrorInfoIfAvailableAndErrorsIsFalse() {

        // given
        String expectedType = UUID.randomUUID().toString();
        String expectedReason = UUID.randomUUID().toString();

        Error error = new Error();
        error.setType(expectedType);
        error.setReason(expectedReason);

        BatchItemResult successfulItem = new BatchItemResult();
        BatchItemResult failed = new BatchItemResult();
        failed.setError(error);

        List<BatchItemResult> items = new ArrayList<>();
        items.add(successfulItem);
        items.add(failed);

        BatchResult result = createTestBatchResult(false, null, items);
        result.withErrorMessage(DEFAULT_TEST_MESSAGE);

        // when
        String actualMessage = result.getErrorMessage();

        // then
        assertFalse(actualMessage.contains(expectedType));
        assertFalse(actualMessage.contains(expectedReason));
        assertEquals(DEFAULT_TEST_MESSAGE, actualMessage);

    }

    @Test
    public void errorMessageContainsBothRootErrorInfoAndFailedItemErrorInfoIfErrorsIsTrue() {

        // given
        String expectedRootType = UUID.randomUUID().toString();
        String expectedRootReason = UUID.randomUUID().toString();

        Error rootError = new Error();
        rootError.setType(expectedRootType);
        rootError.setReason(expectedRootReason);

        String expectedType = UUID.randomUUID().toString();
        String expectedReason = UUID.randomUUID().toString();

        Error error = new Error();
        error.setType(expectedType);
        error.setReason(expectedReason);

        BatchItemResult successfulItem = new BatchItemResult();
        BatchItemResult failed = new BatchItemResult();
        failed.setError(error);

        List<BatchItemResult> items = new ArrayList<>();
        items.add(successfulItem);
        items.add(failed);

        BatchResult result = createTestBatchResult(true, rootError, items);
        result.withErrorMessage(DEFAULT_TEST_MESSAGE);

        // when
        String actualMessage = result.getErrorMessage();

        // then
        assertTrue(actualMessage.contains(expectedRootType));
        assertTrue(actualMessage.contains(expectedRootReason));
        assertTrue(actualMessage.contains(expectedType));
        assertTrue(actualMessage.contains(expectedReason));

    }

    @Test
    public void errorMessageContainsOnlyRootErrorInfoAndFailedItemErrorInfoIfErrorsIsFalse() {

        // given
        String expectedRootType = UUID.randomUUID().toString();
        String expectedRootReason = UUID.randomUUID().toString();

        Error rootError = new Error();
        rootError.setType(expectedRootType);
        rootError.setReason(expectedRootReason);

        String expectedType = UUID.randomUUID().toString();
        String expectedReason = UUID.randomUUID().toString();

        Error error = new Error();
        error.setType(expectedType);
        error.setReason(expectedReason);

        BatchItemResult successfulItem = new BatchItemResult();
        BatchItemResult failed = new BatchItemResult();
        failed.setError(error);

        List<BatchItemResult> items = new ArrayList<>();
        items.add(successfulItem);
        items.add(failed);

        BatchResult result = createTestBatchResult(false, rootError, items);
        result.withErrorMessage(DEFAULT_TEST_MESSAGE);

        // when
        String actualMessage = result.getErrorMessage();

        // then
        assertTrue(actualMessage.contains(expectedRootType));
        assertTrue(actualMessage.contains(expectedRootReason));
        assertFalse(actualMessage.contains(expectedType));
        assertFalse(actualMessage.contains(expectedReason));

    }

    public static BatchResult createTestBatchResult(boolean errors, Error error) {
        return createTestBatchResult(errors, error, null);
    }

    public static BatchResult createTestBatchResult(boolean errors, Error error, List<BatchItemResult> items) {
        return new BatchResult(0, errors, error, DEFAULT_TEST_STATUS, items);
    }

}
