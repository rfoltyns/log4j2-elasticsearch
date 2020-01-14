package org.appenders.log4j2.elasticsearch;

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

import org.apache.logging.log4j.core.Layout;

public class ItemAppenderFactory {

    /**
     * Creates {@link ItemAppender} instance with serializer selected based on given parameters
     *
     * @param messageOnly if true, {@link org.apache.logging.log4j.message.Message} will be serialized,
     *                    {@link org.apache.logging.log4j.core.LogEvent} otherwise
     * @param layout output formatter
     * @param batchDelivery serialization result consumer
     * @return configured {@link ItemAppender}
     */
    public ItemAppender createInstance(boolean messageOnly, Layout<String> layout, BatchDelivery batchDelivery) {

        if (layout instanceof ItemSourceLayout) {
            ItemSourceLayout itemSourceLayout = (ItemSourceLayout) layout;
            return createInstance(messageOnly, itemSourceLayout, batchDelivery);
        }

        if (messageOnly) {
            return new StringAppender(batchDelivery, logEvent -> logEvent.getMessage().getFormattedMessage());
        }

        return new StringAppender(batchDelivery, layout::toSerializable);

    }


    /**
     * Creates {@link ItemSourceAppender} instance with serializer selected based on given parameters
     *
     * @param messageOnly if true, {@code org.apache.logging.log4j.message.Message} will be serialized,
     *                    {@code org.apache.logging.log4j.core.LogEvent} otherwise
     * @param itemSourceLayout output formatter
     * @param batchDelivery serialization result consumer
     * @return configured {@link ItemSourceAppender}
     */
    public ItemSourceAppender createInstance(boolean messageOnly, ItemSourceLayout itemSourceLayout, BatchDelivery batchDelivery) {
        if (messageOnly) {
            return new ItemSourceAppender(batchDelivery, logEvent -> itemSourceLayout.serialize(logEvent.getMessage()));
        }
        return new ItemSourceAppender(batchDelivery, itemSourceLayout::serialize);
    }

}
