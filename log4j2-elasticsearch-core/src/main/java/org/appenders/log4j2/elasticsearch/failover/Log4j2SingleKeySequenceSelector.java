package org.appenders.log4j2.elasticsearch.failover;

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

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;

@Plugin(name = Log4j2SingleKeySequenceSelector.PLUGIN_NAME, category = Node.CATEGORY, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class Log4j2SingleKeySequenceSelector extends SingleKeySequenceSelector {

    static final String PLUGIN_NAME = "SingleKeySequenceSelector";

    public Log4j2SingleKeySequenceSelector(Builder builder) {
        super(builder.sequenceId);
    }

    @PluginBuilderFactory
    public static Log4j2SingleKeySequenceSelector.Builder newBuilder() {
        return new Log4j2SingleKeySequenceSelector.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<SingleKeySequenceSelector> {

        @PluginBuilderAttribute("sequenceId")
        protected long sequenceId;

        @Override
        public final Log4j2SingleKeySequenceSelector build() {

            if (sequenceId <= 0) {
                throw new ConfigurationException("sequenceId must be higher than 0");
            }

            return new Log4j2SingleKeySequenceSelector(this);

        }

        /**
         * @param sequenceId to be used
         * @return this
         */
        public Builder withSequenceId(long sequenceId) {
            this.sequenceId = sequenceId;
            return this;
        }

    }

}
