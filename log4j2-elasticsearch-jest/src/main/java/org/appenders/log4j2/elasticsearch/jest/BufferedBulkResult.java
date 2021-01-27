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

import java.util.List;
import java.util.Optional;

public class BufferedBulkResult {

    static final String NO_FAILED_ITEMS_MESSAGE = "Unable to extract error info from failed items";
    static final String ONE_OR_MORE_ITEMS_FAILED_MESSAGE = "One or more items failed";
    static final String FIRST_ERROR_ITEM_PREFIX = "First error: ";
    static final String ROOT_ERROR_ITEM_PREFIX = "Root error: ";
    static final String BULK_REQUEST_FAILED_MESAGE = "Bulk request failed";
    private static final String SEPARATOR = ". ";

    private final int took;
    private final boolean errors;
    private final BulkError error;
    private final int status;
    private final List<BulkResultItem> items;

    public BufferedBulkResult(int took, boolean errors, BulkError error, int status, List<BulkResultItem> items) {
        this.took = took;
        this.errors = errors;
        this.error = error;
        this.status = status;
        this.items = items;
    }

    public int getTook() {
        return took;
    }

    public boolean isSucceeded() {
        return !this.errors && error == null;
    }

    public BulkError getError() {
        return error;
    }

    public int getStatus() {
        return status;
    }

    public List<BulkResultItem> getItems() {
        return items;
    }

    String getErrorMessage(String defaultMessage) {
        if (isSucceeded()) {
            return defaultMessage;
        }

        StringBuilder sb = new StringBuilder(256);
        if (errors) {
            sb.append(ONE_OR_MORE_ITEMS_FAILED_MESSAGE);
            appendFailedItemErrorMessageIfAvailable(sb.append(SEPARATOR));
        } else {
            sb.append(BULK_REQUEST_FAILED_MESAGE);
        }

        if (getStatus() > 0) {
            sb.append(SEPARATOR).append("status: ").append(getStatus());
        }
        if (getError() != null) {
            sb.append(SEPARATOR).append(ROOT_ERROR_ITEM_PREFIX);
            getError().appendErrorMessage(sb);
        }

        return sb.toString();

    }

    private StringBuilder appendFailedItemErrorMessageIfAvailable(StringBuilder sb) {
        if (getItems() == null) {
            return sb.append(NO_FAILED_ITEMS_MESSAGE);
        }

        Optional<BulkResultItem> firstFailedItem = getItems().stream().filter(item -> item.getBulkError() != null).findFirst();
        if (!firstFailedItem.isPresent()) {
            return sb.append(NO_FAILED_ITEMS_MESSAGE);
        }

        sb.append(FIRST_ERROR_ITEM_PREFIX);
        return firstFailedItem.get().getBulkError().appendErrorMessage(sb);
    }

}
