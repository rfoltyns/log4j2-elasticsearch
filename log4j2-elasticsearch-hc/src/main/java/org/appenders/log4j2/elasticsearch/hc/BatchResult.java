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

import java.util.List;
import java.util.Optional;

public class BatchResult implements Response {

    static final String UNABLE_TO_GET_MORE_INFO = "Unable to extract error info from failed items";
    static final String ONE_OR_MORE_ITEMS_FAILED = "One or more items failed";
    static final String FIRST_FAILED_ITEM_PREFIX = "First failed item: ";
    static final String ROOT_ERROR_PREFIX = "Root error: ";
    static final String REQUEST_FAILED_MESAGE = "Request failed";
    private static final String SEPARATOR = ". ";

    private final int took;
    private final boolean errors;
    private final Error error;
    private final int statusCode;
    private final List<BatchItemResult> items;
    private String errorMessage;
    private int responseCode;

    public BatchResult(int took, boolean errors, Error error, int statusCode, List<BatchItemResult> items) {
        this.took = took;
        this.errors = errors;
        this.error = error;
        this.statusCode = statusCode;
        this.items = items;
    }

    public int getTook() {
        return took;
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

    public BatchResult withResponseCode(int responseCode) {
        this.responseCode = responseCode;
        return this;
    }

    private StringBuilder appendFailedItemErrorMessageIfAvailable(StringBuilder sb) {
        if (getItems() == null) {
            return sb.append(UNABLE_TO_GET_MORE_INFO);
        }

        Optional<BatchItemResult> firstFailedItem = getItems().stream().filter(item -> item.getError() != null).findFirst();
        if (!firstFailedItem.isPresent()) {
            return sb.append(UNABLE_TO_GET_MORE_INFO);
        }

        sb.append(FIRST_FAILED_ITEM_PREFIX);
        return firstFailedItem.get().getError().appendErrorMessage(sb);
    }

    public BatchResult withErrorMessage(String errorMessage) {

        this.errorMessage = errorMessage;

        if (isSucceeded()) {
            return this;
        }

        StringBuilder sb = new StringBuilder(256);
        if (errors) {
            sb.append(ONE_OR_MORE_ITEMS_FAILED);
            appendFailedItemErrorMessageIfAvailable(sb.append(SEPARATOR));
        } else {
            sb.append(REQUEST_FAILED_MESAGE);
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

}
