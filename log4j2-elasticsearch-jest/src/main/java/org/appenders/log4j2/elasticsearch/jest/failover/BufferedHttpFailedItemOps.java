package org.appenders.log4j2.elasticsearch.jest.failover;

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

import io.searchbox.action.AbstractDocumentTargetedAction;
import io.searchbox.core.DocumentResult;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.appenders.log4j2.elasticsearch.failover.FailedItemInfo;
import org.appenders.log4j2.elasticsearch.failover.FailedItemOps;
import org.appenders.log4j2.elasticsearch.failover.FailedItemSource;
import org.appenders.log4j2.elasticsearch.jest.BufferedIndex;

public class BufferedHttpFailedItemOps implements FailedItemOps<AbstractDocumentTargetedAction<DocumentResult>> {

    private static final Logger LOG = StatusLogger.getLogger();

    @Override
    public FailedItemSource createItem(AbstractDocumentTargetedAction<DocumentResult> failed) {
        BufferedIndex failedRequest = (BufferedIndex) failed;
        if (failedRequest.getSource() instanceof FailedItemSource) {
            LOG.trace("Reusing {}", FailedItemSource.class.getSimpleName());
            return (FailedItemSource) failedRequest.getSource();
        }
        return new FailedItemSource<>(failedRequest.getSource(), createInfo(failed));
    }

    @Override
    public FailedItemInfo createInfo(
            AbstractDocumentTargetedAction<DocumentResult> failed
    ) {
        return new FailedItemInfo(failed.getIndex());
    }

}
