package org.appenders.log4j2.elasticsearch.jest;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BufferedBulkResultTest {

    private static final String DEFAULT_TEST_MESSAGE = "default_test_message";
    private static final int DEFAULT_TEST_STATUS = new Random().nextInt(1000) + 1;

    @Test
    public void isSucceededReturnsFalseWhenErrorsIsTrueAndBulkErrorIsNull() {

        // given
        BufferedBulkResult result = createTestBufferedBulkResult(true, null);

        // when
        boolean succeeded = result.isSucceeded();

        // then
        Assert.assertFalse(succeeded);

    }

    @Test
    public void isSucceededReturnsFalseWhenErrorsIsFalseAndBulkErrorIsNotNull() {

        // given
        BufferedBulkResult result = createTestBufferedBulkResult(false, new BulkError());

        // when
        boolean succeeded = result.isSucceeded();

        // then
        Assert.assertFalse(succeeded);

    }

    @Test
    public void isSucceededReturnsFalseWhenErrorsIsTrueAndBulkErrorIsNotNull() {

        // given
        BufferedBulkResult result = createTestBufferedBulkResult(true, new BulkError());

        // when
        boolean succeeded = result.isSucceeded();

        // then
        Assert.assertFalse(succeeded);

    }

    @Test
    public void isSucceededReturnsTrueWhenErrorsIsFalseAndBulkErrorIsNull() {

        // given
        BufferedBulkResult result = createTestBufferedBulkResult(false, null);

        // when
        boolean succeeded = result.isSucceeded();

        // then
        Assert.assertTrue(succeeded);

    }

    @Test
    public void errorMessageDefaultsToProvidedIfRequestIsSucceeeded() {

        // given
        String expectedMessage = UUID.randomUUID().toString();

        BufferedBulkResult result = createTestBufferedBulkResult(false, null);

        // when
        String actualMessage = result.getErrorMessage(expectedMessage);

        // then
        assertEquals(expectedMessage, actualMessage);

    }

    @Test
    public void errorMessageContainsStatusIfAvailableAndNotSucceeded() {

        // given
        String expectedMessage = UUID.randomUUID().toString();

        BufferedBulkResult result = createTestBufferedBulkResult(false, new BulkError());

        // when
        String actualMessage = result.getErrorMessage(expectedMessage);

        // then
        assertTrue(actualMessage.contains("" + DEFAULT_TEST_STATUS));

    }

    @Test
    public void errorMessageDoesNotContainStatusIfAvailableAndNotHigherThanZeroAndNotSucceeded() {

        // given
        String expectedMessage = UUID.randomUUID().toString();

        BufferedBulkResult result = new BufferedBulkResult(0, false, new BulkError(), 0, null);

        // when
        String actualMessage = result.getErrorMessage(expectedMessage);

        // then
        assertFalse(actualMessage.contains("" + DEFAULT_TEST_STATUS));

    }

    @Test
    public void errorMessageDoesNotContainStatusIfHigherThanZeroAndSucceeded() {

        // given
        String expectedMessage = UUID.randomUUID().toString();

        BufferedBulkResult result = new BufferedBulkResult(0, false, null, 1, null);

        // when
        String actualMessage = result.getErrorMessage(expectedMessage);

        // then
        assertFalse(actualMessage.contains("" + DEFAULT_TEST_STATUS));

    }

    @Test
    public void errorMessageDoesNotContainStatusIfStatusEqualsZeroAndSucceeded() {

        // given
        String expectedMessage = UUID.randomUUID().toString();

        BufferedBulkResult result = new BufferedBulkResult(0, false, null, 0, null);;

        // when
        String actualMessage = result.getErrorMessage(expectedMessage);

        // then
        assertFalse(actualMessage.contains("" + DEFAULT_TEST_STATUS));

    }

    @Test
    public void errorMessageContainsRootBulkErrorInfoIfAvailable() {

        // given
        String expectedType = UUID.randomUUID().toString();
        String expectedReason = UUID.randomUUID().toString();

        BulkError error = new BulkError();
        error.setType(expectedType);
        error.setReason(expectedReason);

        BufferedBulkResult result = createTestBufferedBulkResult(false, error);

        // when
        String actualMessage = result.getErrorMessage(DEFAULT_TEST_MESSAGE);

        // then
        assertTrue(actualMessage.contains(expectedType));
        assertTrue(actualMessage.contains(expectedReason));

    }

    @Test
    public void errorMessageContainsRootBulkErrorCausedByInfoIfAvailable() {

        // given
        String expectedType = UUID.randomUUID().toString();
        String expectedReason = UUID.randomUUID().toString();

        String expectedCausedByType = UUID.randomUUID().toString();
        String expectedCausedByReason = UUID.randomUUID().toString();

        BulkError causedByError = new BulkError();
        causedByError.setType(expectedCausedByType);
        causedByError.setReason(expectedCausedByReason);

        BulkError error = new BulkError();
        error.setType(expectedType);
        error.setReason(expectedReason);
        error.setCausedBy(causedByError);

        BufferedBulkResult result = createTestBufferedBulkResult(false, error);

        // when
        String actualMessage = result.getErrorMessage(DEFAULT_TEST_MESSAGE);

        // then
        assertTrue(actualMessage.contains(expectedCausedByType));
        assertTrue(actualMessage.contains(expectedCausedByReason));
        assertFalse(actualMessage.contains(expectedType));
        assertFalse(actualMessage.contains(expectedReason));


    }

    @Test
    public void errorMessageContainsInfoWhenItemsListIsNullAndErrorsIsTrue() {

        // given
        String expectedMessage = BufferedBulkResult.NO_FAILED_ITEMS_MESSAGE;

        BufferedBulkResult result = createTestBufferedBulkResult(true, null, null);

        // when
        String actualMessage = result.getErrorMessage(DEFAULT_TEST_MESSAGE);

        // then
        assertTrue(actualMessage.contains(expectedMessage));

    }

    @Test
    public void errorMessageContainsInfoWhenItemsListIsEmptyAndErrorsIsTrue() {

        // given
        String expectedMessage = BufferedBulkResult.NO_FAILED_ITEMS_MESSAGE;

        BufferedBulkResult result = createTestBufferedBulkResult(true, null, new ArrayList<>());

        // when
        String actualMessage = result.getErrorMessage(DEFAULT_TEST_MESSAGE);

        // then
        assertTrue(actualMessage.contains(expectedMessage));

    }

    @Test
    public void errorMessageContainsFailedItemBulkErrorInfoIfAvailableAndErrorsIsTrue() {

        // given
        String expectedType = UUID.randomUUID().toString();
        String expectedReason = UUID.randomUUID().toString();

        BulkError error = new BulkError();
        error.setType(expectedType);
        error.setReason(expectedReason);

        BulkResultItem successfulItem = new BulkResultItem();
        BulkResultItem failed = new BulkResultItem();
        failed.setBulkError(error);

        List<BulkResultItem> items = new ArrayList<>();
        items.add(successfulItem);
        items.add(failed);

        BufferedBulkResult result = createTestBufferedBulkResult(true, null, items);

        // when
        String actualMessage = result.getErrorMessage(DEFAULT_TEST_MESSAGE);

        // then
        assertTrue(actualMessage.contains(expectedType));
        assertTrue(actualMessage.contains(expectedReason));

    }

    @Test
    public void errorMessageDoesNotContainFailedItemBulkErrorInfoIfAvailableAndErrorsIsFalse() {

        // given
        String expectedType = UUID.randomUUID().toString();
        String expectedReason = UUID.randomUUID().toString();

        BulkError error = new BulkError();
        error.setType(expectedType);
        error.setReason(expectedReason);

        BulkResultItem successfulItem = new BulkResultItem();
        BulkResultItem failed = new BulkResultItem();
        failed.setBulkError(error);

        List<BulkResultItem> items = new ArrayList<>();
        items.add(successfulItem);
        items.add(failed);

        BufferedBulkResult result = createTestBufferedBulkResult(false, null, items);

        // when
        String actualMessage = result.getErrorMessage(DEFAULT_TEST_MESSAGE);

        // then
        assertFalse(actualMessage.contains(expectedType));
        assertFalse(actualMessage.contains(expectedReason));
        assertEquals(DEFAULT_TEST_MESSAGE, actualMessage);

    }

    @Test
    public void errorMessageContainsBothRootBulkErrorInfoAndFailedItemBulkErrorInfoIfErrorsIsTrue() {

        // given
        String expectedRootType = UUID.randomUUID().toString();
        String expectedRootReason = UUID.randomUUID().toString();

        BulkError rootError = new BulkError();
        rootError.setType(expectedRootType);
        rootError.setReason(expectedRootReason);

        String expectedType = UUID.randomUUID().toString();
        String expectedReason = UUID.randomUUID().toString();

        BulkError error = new BulkError();
        error.setType(expectedType);
        error.setReason(expectedReason);

        BulkResultItem successfulItem = new BulkResultItem();
        BulkResultItem failed = new BulkResultItem();
        failed.setBulkError(error);

        List<BulkResultItem> items = new ArrayList<>();
        items.add(successfulItem);
        items.add(failed);

        BufferedBulkResult result = createTestBufferedBulkResult(true, rootError, items);

        // when
        String actualMessage = result.getErrorMessage(DEFAULT_TEST_MESSAGE);

        // then
        assertTrue(actualMessage.contains(expectedRootType));
        assertTrue(actualMessage.contains(expectedRootReason));
        assertTrue(actualMessage.contains(expectedType));
        assertTrue(actualMessage.contains(expectedReason));

    }

    @Test
    public void errorMessageContainsOnlyRootBulkErrorInfoAndFailedItemBulkErrorInfoIfErrorsIsFalse() {

        // given
        String expectedRootType = UUID.randomUUID().toString();
        String expectedRootReason = UUID.randomUUID().toString();

        BulkError rootError = new BulkError();
        rootError.setType(expectedRootType);
        rootError.setReason(expectedRootReason);

        String expectedType = UUID.randomUUID().toString();
        String expectedReason = UUID.randomUUID().toString();

        BulkError error = new BulkError();
        error.setType(expectedType);
        error.setReason(expectedReason);

        BulkResultItem successfulItem = new BulkResultItem();
        BulkResultItem failed = new BulkResultItem();
        failed.setBulkError(error);

        List<BulkResultItem> items = new ArrayList<>();
        items.add(successfulItem);
        items.add(failed);

        BufferedBulkResult result = createTestBufferedBulkResult(false, rootError, items);

        // when
        String actualMessage = result.getErrorMessage(DEFAULT_TEST_MESSAGE);

        // then
        assertTrue(actualMessage.contains(expectedRootType));
        assertTrue(actualMessage.contains(expectedRootReason));
        assertFalse(actualMessage.contains(expectedType));
        assertFalse(actualMessage.contains(expectedReason));

    }

    private BufferedBulkResult createTestBufferedBulkResult(boolean errors, BulkError error) {
        return createTestBufferedBulkResult(errors, error, null);
    }

    private BufferedBulkResult createTestBufferedBulkResult(boolean errors, BulkError error, List<BulkResultItem> items) {
        return new BufferedBulkResult(0, errors, error, DEFAULT_TEST_STATUS, items);
    }

}
