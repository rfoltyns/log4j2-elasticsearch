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


import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rolling.PatternProcessor;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;

import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Time-based rolling index name formatter. Caches formatted index name between rolls to minimize overhead.
 * <p>
 * Format: {@code <indexName>-<datePattern>}
 * <p>
 * This class is NOT thread-safe!
 */
@Plugin(name = "RollingIndexName", category = Node.CATEGORY, elementType = IndexNameFormatter.ELEMENT_TYPE, printObject = true)
public class RollingIndexNameFormatter implements IndexNameFormatter<LogEvent> {

    public static final String DEFAULT_SEPARATOR = "-";

    private final String indexName;
    private String currentName;
    private final String separator;
    private long nextRolloverTime;
    private final AtomicBoolean rollingOver = new AtomicBoolean();

    /**
     * Pattern parser. Provides next rollover time.
     */
    private final PatternProcessor patternProcessor;

    /**
     * Alternative formatter used during and right after the rollover due to race conditions caused by PatternProcessor internal state
     */
    private final FastDateFormat fastDateFormat;

    /**
     * Initial buffer capacity (to avoid resizing)
     */
    private final int defaultBufferSize = 32;
    private long currentFileTime;

    protected RollingIndexNameFormatter(String indexName, String pattern, long initTimeInMillis, TimeZone timeZone) {
        this(indexName, pattern, initTimeInMillis, timeZone, DEFAULT_SEPARATOR);
    }

    protected RollingIndexNameFormatter(String indexName, String pattern, long initTimeInMillis, TimeZone timeZone, String separator) {
        this.indexName = indexName;
        this.fastDateFormat = FastDateFormat.getInstance(pattern, timeZone);
        this.patternProcessor = createPatternProcessor(pattern);
        this.separator = separator;
        this.currentName = doFormat(indexName, initTimeInMillis);

        long previousTime = this.patternProcessor.getNextTime(initTimeInMillis, -1, false);
        this.patternProcessor.setPrevFileTime(previousTime);
        this.nextRolloverTime = this.patternProcessor.getNextTime(initTimeInMillis, 0, false);
        this.currentFileTime = this.nextRolloverTime;
    }

    protected PatternProcessor createPatternProcessor(String pattern) {
        return new PatternProcessor("%d{" + pattern + "}");
    }

    long getNextRolloverTime() {
        return nextRolloverTime;
    }

    @Override
    public final String format(LogEvent event) {
        long eventTimeInMillis = event.getTimeMillis();

        // handle "old" events that arrived in separate threads after rollover
        if (eventTimeInMillis < currentFileTime) {
            return doFormat(indexName, eventTimeInMillis);
        }

        // rollover
        while (eventTimeInMillis >= nextRolloverTime) {
            if (rollingOver.compareAndSet(false, true)) {
                rollover(indexName, eventTimeInMillis);
                rollingOver.set(false);
                continue;
            }
            break;
        }

        // happy path - have to check for pending rollover to avoid race conditions
        if (!rollingOver.get()) {
            return currentName;
        }

        // fail-safe for pending rollover
        return doFormat(indexName, eventTimeInMillis);
    }

    private void rollover(String indexName, long eventTimeInMillis) {
        nextRolloverTime = patternProcessor.getNextTime(eventTimeInMillis, 1, false);
        currentFileTime = patternProcessor.getNextTime(eventTimeInMillis, 0, false);
        currentName = doFormat(indexName, eventTimeInMillis);
    }

    private String doFormat(String indexName, long timeInMillis) {
        return fastDateFormat.format(timeInMillis, buffer(indexName).append(separator)).toString();
    }

    private StringBuilder buffer(String indexName) {
        return new StringBuilder(defaultBufferSize).append(indexName);
    }

    @PluginBuilderFactory
    public static RollingIndexNameFormatter.Builder newBuilder() {
        return new RollingIndexNameFormatter.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<RollingIndexNameFormatter> {

        public static final String DEFAULT_TIME_ZONE = TimeZone.getDefault().getID();

        @PluginBuilderAttribute
        @Required(message = "No indexName provided for RollingIndexName")
        private String indexName;

        @PluginBuilderAttribute
        @Required(message = "No pattern provided for RollingIndexName")
        private String pattern;

        @PluginBuilderAttribute
        private String timeZone = DEFAULT_TIME_ZONE;

        @PluginBuilderAttribute
        private String separator = DEFAULT_SEPARATOR;

        @Override
        public RollingIndexNameFormatter build() {
            if (indexName == null) {
                throw new ConfigurationException("No indexName provided for RollingIndexName");
            }
            if (pattern == null) {
                throw new ConfigurationException("No pattern provided for RollingIndexName");
            }
            return new RollingIndexNameFormatter(indexName, pattern, getInitTimeInMillis(), TimeZone.getTimeZone(timeZone), separator);
        }

        public Builder withIndexName(String indexName) {
            this.indexName = indexName;
            return this;
        }

        public Builder withPattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        public Builder withTimeZone(String timeZone) {
            this.timeZone = timeZone;
            return this;
        }

        public Builder withSeparator(String separator) {
            this.separator = separator;
            return this;
        }

        long getInitTimeInMillis() {
            return System.currentTimeMillis();
        }
    }

}
