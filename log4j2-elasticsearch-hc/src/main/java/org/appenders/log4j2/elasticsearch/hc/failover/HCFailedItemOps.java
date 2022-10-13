package org.appenders.log4j2.elasticsearch.hc.failover;

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

import org.appenders.log4j2.elasticsearch.failover.FailedItemInfo;
import org.appenders.log4j2.elasticsearch.failover.FailedItemOps;
import org.appenders.log4j2.elasticsearch.failover.FailedItemSource;
import org.appenders.log4j2.elasticsearch.hc.IndexRequest;

import static org.appenders.core.logging.InternalLogging.getLogger;

public class HCFailedItemOps implements FailedItemOps<IndexRequest> {

    /**
     * @param failed failed request
     * @return FailedItemSource underlying {@link FailedItemSource} being processed or new one
     */
    @Override
    public FailedItemSource createItem(IndexRequest failed) {

        if (failed.getSource() instanceof FailedItemSource) {
            getLogger().trace("Reusing {}", FailedItemSource.class.getSimpleName());
            return (FailedItemSource) failed.getSource();
        }

        return new FailedItemSource<>(failed.getSource(), createInfo(failed));

    }

    @Override
    public FailedItemInfo createInfo(IndexRequest failed) {
        return new FailedItemInfo(failed.getIndex());
    }

}
