package org.appenders.log4j2.elasticsearch.jest;

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

import io.searchbox.action.BulkableAction;
import io.searchbox.core.Bulk;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ExtendedBulk extends Bulk {

    protected ExtendedBulk(Builder builder) {
        super(builder);
        super.bulkableActions = new ConcurrentLinkedQueue<>(builder.actions);
    }

    public static class Builder extends Bulk.Builder {

        private Collection<BulkableAction> actions = new ConcurrentLinkedQueue<>();

        public Builder addAction(BulkableAction action) {
            this.actions.add(action);
            return this;
        }

        public Builder addAction(Collection<? extends BulkableAction> actions) {
            this.actions.addAll(actions);
            return this;
        }

        public ExtendedBulk build() {
            return new ExtendedBulk(this);
        }

    }

}
