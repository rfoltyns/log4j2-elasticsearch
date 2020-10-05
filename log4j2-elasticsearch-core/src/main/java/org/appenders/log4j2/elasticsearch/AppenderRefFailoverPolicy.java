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


import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.config.AppenderControl;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.impl.DefaultLogEventFactory;
import org.apache.logging.log4j.message.SimpleMessage;
import org.appenders.log4j2.elasticsearch.failover.FailedItemSource;

/**
 * Allows to redirect failed logs to another appender.
 */
@Plugin(name = "AppenderRefFailoverPolicy", category = Node.CATEGORY, elementType = FailoverPolicy.ELEMENT_TYPE, printObject = true)
public class AppenderRefFailoverPolicy implements FailoverPolicy<String> {

    protected final AppenderRef appenderRef;
    private final Configuration configuration;

    protected AppenderControl appenderControl;

    protected AppenderRefFailoverPolicy(AppenderRef appenderRef, Configuration configuration) {
        this.appenderRef = appenderRef;
        this.configuration = configuration;
    }

    @Override
    public final void deliver(String failedPayload) {

        // Since Configuration is not complete during the startup, let's resolve lazily here
        resolveAppender();

        doDeliver(failedPayload);
    }

    @Override
    public void deliver(FailedItemSource failedPayload) {

        // Since Configuration is not complete during the startup, let's resolve lazily here
        this.resolveAppender();

        doDeliver(failedPayload.toString());
    }

    /**
     * Extension point - let's allow to customize e.g.: LogEvent type or any other param
     *
     * @param failedPayload payload to be handled
     */
    protected void doDeliver(String failedPayload) {
        appenderControl.callAppender(DefaultLogEventFactory.getInstance().createEvent(appenderRef.getRef(),
                null,
                getClass().getName(),
                appenderRef.getLevel(),
                new SimpleMessage(failedPayload),
                null,
                null));
    }

    private void resolveAppender() {
        if (appenderControl == null) {
            Appender appender = configuration.getAppender(appenderRef.getRef());
            if (appender == null) {
                throw new ConfigurationException("No failover appender named " + appenderRef.getRef() + " found");
            }
            appenderControl = new AppenderControl(appender, appenderRef.getLevel(), appenderRef.getFilter());
        }
    }

    @PluginBuilderFactory
    public static AppenderRefFailoverPolicy.Builder newBuilder() {
        return new AppenderRefFailoverPolicy.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<AppenderRefFailoverPolicy> {

        @PluginElement("AppenderRef")
        @Required(message = "No appender specified for AppenderRefFailoverPolicy")
        private AppenderRef appenderRef;

        @PluginConfiguration
        private Configuration configuration;

        @Override
        public AppenderRefFailoverPolicy build() {
            return new AppenderRefFailoverPolicy(appenderRef, configuration);
        }

        public Builder withAppenderRef(AppenderRef appenderRef) {
            this.appenderRef = appenderRef;
            return this;
        }

        public Builder withConfiguration(Configuration configuration) {
            this.configuration = configuration;
            return this;
        }
    }

}
