package org.appenders.log4j2.elasticsearch.hc.backoff;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.appenders.log4j2.elasticsearch.backoff.BatchLimitBackoffPolicy;
import org.appenders.log4j2.elasticsearch.hc.BatchRequest;

@Plugin(name = Log4j2BatchLimitBackoffPolicy.PLUGIN_NAME, category = Node.CATEGORY, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class Log4j2BatchLimitBackoffPolicy extends BatchLimitBackoffPolicy<BatchRequest> {

    static final String PLUGIN_NAME = "BatchLimitBackoffPolicy";

    public Log4j2BatchLimitBackoffPolicy(int maxBatchesInFlight) {
        super(maxBatchesInFlight);
    }

    @PluginBuilderFactory
    public static Log4j2BatchLimitBackoffPolicy.Builder newBuilder() {
        return new Log4j2BatchLimitBackoffPolicy.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<BatchLimitBackoffPolicy> {

        static final int DEFAULT_MAX_BATCHES_IN_FLIGHT = 8;

        @PluginAttribute("maxBatchesInFlight")
        protected int maxBatchesInFlight = DEFAULT_MAX_BATCHES_IN_FLIGHT;

        @Override
        public final BatchLimitBackoffPolicy build() {

            if (maxBatchesInFlight <= 0) {
                throw new ConfigurationException("maxBatchesInFlight must be higher than 0 for " +
                        BatchLimitBackoffPolicy.class.getSimpleName());
            }

            return new Log4j2BatchLimitBackoffPolicy(maxBatchesInFlight);

        }

        /**
         * Sets the limit of allowed concurrent batches
         *
         * @param maxBatchesInFlight max number of concurrent batches
         * @return this
         */
        public Builder withMaxBatchesInFlight(int maxBatchesInFlight) {
            this.maxBatchesInFlight = maxBatchesInFlight;
            return this;
        }

    }

}
