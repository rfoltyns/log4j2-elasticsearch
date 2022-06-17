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

import java.util.List;
import java.util.Optional;

public class BatchResult implements Response {

    static final BatchResult INPUT_STREAM_NULL;

    static {
        final Error error = new Error();
        error.setReason("inputStream is null");
        INPUT_STREAM_NULL = new CopyingBatchResult(new BatchResult(0, false, error, 500, null));
    }

    static final String UNABLE_TO_GET_MORE_INFO = "Unable to extract error info from failed items";
    static final String ONE_OR_MORE_ITEMS_FAILED = "One or more items failed";
    static final String FIRST_FAILED_ITEM_PREFIX = "First failed item: ";
    static final String ROOT_ERROR_PREFIX = "Root error: ";
    private static final String SEPARATOR = ". ";

    private final int took;
    private final boolean errors;
    private final Error error;
    private final int statusCode;
    private final List<BatchItemResult> items;
    private String errorMessage;
    private int responseCode;

    public BatchResult(final int took, final boolean errors, final Error error, final int statusCode, final List<BatchItemResult> items) {
        this.took = took;
        this.errors = errors;
        this.error = error;
        this.statusCode = statusCode;
        this.items = items;
    }

    public int getTook() {
        return took;
    }

    public boolean hasErrors() {
        return errors;
    }

    /**
     * @return true if {@link #errors} is false and {@link #error is null}, false otherwise
     */
    public boolean isSucceeded() {
        return !this.errors && error == null;
    }

    public Error getError() {
        return error;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public List<BatchItemResult> getItems() {
        return items;
    }

    @Override
    public int getResponseCode() {
        return responseCode;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    public BatchResult withResponseCode(final int responseCode) {
        this.responseCode = responseCode;
        return this;
    }

    private StringBuilder appendFailedItemErrorMessageIfAvailable(final StringBuilder sb) {
        if (getItems() == null) {
            return sb.append(UNABLE_TO_GET_MORE_INFO);
        }

        final Optional<BatchItemResult> firstFailedItem = getItems().stream().filter(item -> item.getError() != null).findFirst();
        if (!firstFailedItem.isPresent()) {
            return sb.append(UNABLE_TO_GET_MORE_INFO);
        }

        sb.append(FIRST_FAILED_ITEM_PREFIX);
        return firstFailedItem.get().getError().appendErrorMessage(sb);
    }

    public BatchResult withErrorMessage(final String errorMessage) {

        this.errorMessage = errorMessage;

        if (isSucceeded()) {
            return this;
        }

        final StringBuilder sb = new StringBuilder(256);

        sb.append(errorMessage);

        if (errors) {
            sb.append(SEPARATOR).append(ONE_OR_MORE_ITEMS_FAILED);
            appendFailedItemErrorMessageIfAvailable(sb.append(SEPARATOR));
        }

        if (statusCode > 0) {
            sb.append(SEPARATOR).append("status: ").append(statusCode);
        }
        if (getError() != null) {
            sb.append(SEPARATOR).append(ROOT_ERROR_PREFIX);
            getError().appendErrorMessage(sb);
        }

        this.errorMessage = sb.toString();
        return this;
    }

    private static class CopyingBatchResult extends BatchResult {

        CopyingBatchResult(final BatchResult batchResult) {
            super(batchResult.took,
                    batchResult.errors,
                    batchResult.error,
                    batchResult.statusCode,
                    batchResult.items);
        }

        @Override
        public final BatchResult withErrorMessage(final String errorMessage) {
            return new BatchResult(getTook(), hasErrors(), getError(), getStatusCode(), getItems())
                    .withErrorMessage(errorMessage);
        }

        @Override
        public final BatchResult withResponseCode(final int responseCode) {
            return new BatchResult(getTook(), hasErrors(), getError(), getResponseCode(), getItems())
                    .withResponseCode(responseCode);
        }

    }

}
