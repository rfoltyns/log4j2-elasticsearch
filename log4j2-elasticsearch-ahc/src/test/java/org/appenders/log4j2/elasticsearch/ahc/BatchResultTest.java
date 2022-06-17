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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BatchResultTest {

    private static final String DEFAULT_TEST_MESSAGE = "default_test_message";
    private static final int DEFAULT_TEST_STATUS = new Random().nextInt(1000) + 1;

    @Test
    public void isSucceededReturnsFalseWhenErrorsIsTrueAndErrorIsNull() {

        // given
        final BatchResult result = createTestBatchResult(true, null);

        // when
        final boolean succeeded = result.isSucceeded();

        // then
        assertFalse(succeeded);

    }

    @Test
    public void isSucceededReturnsFalseWhenErrorsIsFalseAndErrorIsNotNull() {

        // given
        final BatchResult result = createTestBatchResult(false, new Error());

        // when
        final boolean succeeded = result.isSucceeded();

        // then
        assertFalse(succeeded);

    }

    @Test
    public void isSucceededReturnsFalseWhenErrorsIsTrueAndErrorIsNotNull() {

        // given
        final BatchResult result = createTestBatchResult(true, new Error());

        // when
        final boolean succeeded = result.isSucceeded();
        final boolean hasErrors = result.hasErrors();

        // then
        assertFalse(succeeded);
        assertTrue(hasErrors);

    }

    @Test
    public void isSucceededReturnsTrueWhenErrorsIsFalseAndErrorIsNull() {

        // given
        final BatchResult result = createTestBatchResult(false, null);

        // when
        final boolean succeeded = result.isSucceeded();
        final boolean hasErrors = result.hasErrors();

        // then
        assertTrue(succeeded);
        assertFalse(hasErrors);

    }

    @Test
    public void errorMessageDefaultsToProvidedIfRequestIsSucceeeded() {

        // given
        final String expectedMessage = UUID.randomUUID().toString();

        final BatchResult result = createTestBatchResult(false, null);
        result.withErrorMessage(expectedMessage);

        // when
        final String actualMessage = result.getErrorMessage();

        // then
        assertEquals(expectedMessage, actualMessage);

    }

    @Test
    public void errorMessageContainsStatusIfAvailableAndNotSucceeded() {

        // given
        final String expectedMessage = UUID.randomUUID().toString();

        final BatchResult result = createTestBatchResult(false, new Error());
        result.withErrorMessage(expectedMessage);

        // when
        final String actualMessage = result.getErrorMessage();

        // then
        assertTrue(actualMessage.contains("" + DEFAULT_TEST_STATUS));

    }

    @Test
    public void errorMessageDoesNotContainStatusIfAvailableAndNotHigherThanZeroAndNotSucceeded() {

        // given
        final String expectedMessage = "test_error_message";

        final BatchResult result = new BatchResult(0, false, new Error(), 0, null);
        result.withErrorMessage(expectedMessage);

        // when
        final String actualMessage = result.getErrorMessage();

        // then
        assertFalse(actualMessage.contains("" + 0));

    }

    @Test
    public void errorMessageDoesNotContainStatusIfHigherThanZeroAndSucceeded() {

        // given
        final String expectedMessage = "error_message";

        final BatchResult result = new BatchResult(0, false, null, 1, null);
        result.withErrorMessage(expectedMessage);

        // when
        final String actualMessage = result.getErrorMessage();

        // then
        assertFalse(actualMessage.contains("" + 1));

    }

    @Test
    public void errorMessageDoesNotContainStatusIfStatusEqualsZeroAndSucceeded() {

        // given
        final String expectedMessage = "error_message";

        final BatchResult result = new BatchResult(0, false, null, 0, null);
        result.withErrorMessage(expectedMessage);

        // when
        final String actualMessage = result.getErrorMessage();

        // then
        assertFalse(actualMessage.contains("" + 0));

    }

    @Test
    public void errorMessageContainsRootErrorInfoIfAvailable() {

        // given
        final String expectedType = UUID.randomUUID().toString();
        final String expectedReason = UUID.randomUUID().toString();

        final Error error = new Error();
        error.setType(expectedType);
        error.setReason(expectedReason);

        final BatchResult result = createTestBatchResult(false, error);
        result.withErrorMessage(DEFAULT_TEST_MESSAGE);

        // when
        final String actualMessage = result.getErrorMessage();

        // then
        assertTrue(actualMessage.contains(expectedType));
        assertTrue(actualMessage.contains(expectedReason));

    }

    @Test
    public void errorMessageContainsRootErrorCausedByInfoIfAvailable() {

        // given
        final String expectedType = UUID.randomUUID().toString();
        final String expectedReason = UUID.randomUUID().toString();

        final String expectedCausedByType = UUID.randomUUID().toString();
        final String expectedCausedByReason = UUID.randomUUID().toString();

        final Error causedByError = new Error();
        causedByError.setType(expectedCausedByType);
        causedByError.setReason(expectedCausedByReason);

        final Error error = new Error();
        error.setType(expectedType);
        error.setReason(expectedReason);
        error.setCausedBy(causedByError);

        final BatchResult result = createTestBatchResult(false, error);
        result.withErrorMessage(DEFAULT_TEST_MESSAGE);

        // when
        final String actualMessage = result.getErrorMessage();

        // then
        assertTrue(actualMessage.contains(expectedCausedByType));
        assertTrue(actualMessage.contains(expectedCausedByReason));
        assertFalse(actualMessage.contains(expectedType));
        assertFalse(actualMessage.contains(expectedReason));


    }

    @Test
    public void errorMessageContainsInfoWhenItemsListIsNullAndErrorsIsTrue() {

        // given
        final String expectedMessage = BatchResult.UNABLE_TO_GET_MORE_INFO;

        final BatchResult result = createTestBatchResult(true, null, null);
        result.withErrorMessage(expectedMessage);

        // when
        final String actualMessage = result.getErrorMessage();

        // then
        assertTrue(actualMessage.contains(expectedMessage));

    }

    @Test
    public void errorMessageContainsInfoWhenItemsListIsEmptyAndErrorsIsTrue() {

        // given
        final String expectedMessage = BatchResult.UNABLE_TO_GET_MORE_INFO;

        final BatchResult result = createTestBatchResult(true, null, new ArrayList<>());
        result.withErrorMessage(expectedMessage);

        // when
        final String actualMessage = result.getErrorMessage();

        // then
        assertTrue(actualMessage.contains(expectedMessage));

    }

    @Test
    public void errorMessageContainsFailedItemErrorInfoIfAvailableAndErrorsIsTrue() {

        // given
        final String expectedType = UUID.randomUUID().toString();
        final String expectedReason = UUID.randomUUID().toString();

        final Error error = new Error();
        error.setType(expectedType);
        error.setReason(expectedReason);

        final BatchItemResult successfulItem = new BatchItemResult();
        final BatchItemResult failed = new BatchItemResult();
        failed.setError(error);

        final List<BatchItemResult> items = new ArrayList<>();
        items.add(successfulItem);
        items.add(failed);

        final BatchResult result = createTestBatchResult(true, null, items);
        result.withErrorMessage(DEFAULT_TEST_MESSAGE);

        // when
        final String actualMessage = result.getErrorMessage();

        // then
        assertTrue(actualMessage.contains(expectedType));
        assertTrue(actualMessage.contains(expectedReason));

    }

    @Test
    public void errorMessageDoesNotContainFailedItemErrorInfoIfAvailableAndErrorsIsFalse() {

        // given
        final String expectedType = UUID.randomUUID().toString();
        final String expectedReason = UUID.randomUUID().toString();

        final Error error = new Error();
        error.setType(expectedType);
        error.setReason(expectedReason);

        final BatchItemResult successfulItem = new BatchItemResult();
        final BatchItemResult failed = new BatchItemResult();
        failed.setError(error);

        final List<BatchItemResult> items = new ArrayList<>();
        items.add(successfulItem);
        items.add(failed);

        final BatchResult result = createTestBatchResult(false, null, items);
        result.withErrorMessage(DEFAULT_TEST_MESSAGE);

        // when
        final String actualMessage = result.getErrorMessage();

        // then
        assertFalse(actualMessage.contains(expectedType));
        assertFalse(actualMessage.contains(expectedReason));
        assertEquals(DEFAULT_TEST_MESSAGE, actualMessage);

    }

    @Test
    public void errorMessageContainsBothRootErrorInfoAndFailedItemErrorInfoIfErrorsIsTrue() {

        // given
        final String expectedRootType = UUID.randomUUID().toString();
        final String expectedRootReason = UUID.randomUUID().toString();

        final Error rootError = new Error();
        rootError.setType(expectedRootType);
        rootError.setReason(expectedRootReason);

        final String expectedType = UUID.randomUUID().toString();
        final String expectedReason = UUID.randomUUID().toString();

        final Error error = new Error();
        error.setType(expectedType);
        error.setReason(expectedReason);

        final BatchItemResult successfulItem = new BatchItemResult();
        final BatchItemResult failed = new BatchItemResult();
        failed.setError(error);

        final List<BatchItemResult> items = new ArrayList<>();
        items.add(successfulItem);
        items.add(failed);

        final BatchResult result = createTestBatchResult(true, rootError, items);
        result.withErrorMessage(DEFAULT_TEST_MESSAGE);

        // when
        final String actualMessage = result.getErrorMessage();

        // then
        assertTrue(actualMessage.contains(expectedRootType));
        assertTrue(actualMessage.contains(expectedRootReason));
        assertTrue(actualMessage.contains(expectedType));
        assertTrue(actualMessage.contains(expectedReason));

    }

    @Test
    public void errorMessageContainsOnlyRootErrorInfoAndFailedItemErrorInfoIfErrorsIsFalse() {

        // given
        final String expectedRootType = UUID.randomUUID().toString();
        final String expectedRootReason = UUID.randomUUID().toString();

        final Error rootError = new Error();
        rootError.setType(expectedRootType);
        rootError.setReason(expectedRootReason);

        final String expectedType = UUID.randomUUID().toString();
        final String expectedReason = UUID.randomUUID().toString();

        final Error error = new Error();
        error.setType(expectedType);
        error.setReason(expectedReason);

        final BatchItemResult successfulItem = new BatchItemResult();
        final BatchItemResult failed = new BatchItemResult();
        failed.setError(error);

        final List<BatchItemResult> items = new ArrayList<>();
        items.add(successfulItem);
        items.add(failed);

        final BatchResult result = createTestBatchResult(false, rootError, items);
        result.withErrorMessage(DEFAULT_TEST_MESSAGE);

        // when
        final String actualMessage = result.getErrorMessage();

        // then
        assertTrue(actualMessage.contains(expectedRootType));
        assertTrue(actualMessage.contains(expectedRootReason));
        assertFalse(actualMessage.contains(expectedType));
        assertFalse(actualMessage.contains(expectedReason));

    }

    @Test
    public void inputStreamNullResultIsImmutable() {

        // given
        final BatchResult initial = BatchResult.INPUT_STREAM_NULL;
        final int expectedResponseCode = new Random().nextInt(100) + 1;
        final String expectedErrorMessage = UUID.randomUUID().toString();

        // when
        final BatchResult withErrorMessage = initial.withErrorMessage(expectedErrorMessage);
        final BatchResult withResponseCode = initial.withResponseCode(expectedResponseCode);

        // then
        assertNotSame(initial, withResponseCode);
        assertNotSame(initial, withErrorMessage);
        assertNotSame(withResponseCode, withErrorMessage);
        assertEquals(expectedResponseCode, withResponseCode.getResponseCode());
        assertEquals(initial.getStatusCode(), withErrorMessage.getStatusCode());
        assertEquals(initial.getErrorMessage(), withResponseCode.getErrorMessage());
        assertThat(withErrorMessage.getErrorMessage(), startsWith(expectedErrorMessage));

    }

    public static BatchResult createTestBatchResult(final boolean errors, final Error error) {
        return createTestBatchResult(errors, error, null);
    }

    public static BatchResult createTestBatchResult(final boolean errors, final Error error, final List<BatchItemResult> items) {
        return new BatchResult(0, errors, error, DEFAULT_TEST_STATUS, items);
    }

}
