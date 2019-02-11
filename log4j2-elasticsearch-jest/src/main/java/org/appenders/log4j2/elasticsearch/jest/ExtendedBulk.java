package org.appenders.log4j2.elasticsearch.jest;

import io.searchbox.action.BulkableAction;
import io.searchbox.core.Bulk;
import org.apache.logging.log4j.status.StatusLogger;

import java.util.*;
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
