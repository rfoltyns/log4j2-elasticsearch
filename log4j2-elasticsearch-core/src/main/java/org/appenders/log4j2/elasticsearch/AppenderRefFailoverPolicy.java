package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * %%
 * Copyright (C) 2017 Rafal Foltynski
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.config.*;
import org.apache.logging.log4j.core.config.plugins.*;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.impl.DefaultLogEventFactory;
import org.apache.logging.log4j.message.SimpleMessage;

/**
 * Allows to redirect the failed logs to another appender.
 */
@Plugin(name = "AppenderRefFailoverPolicy", category = Node.CATEGORY, elementType = Appender.ELEMENT_TYPE, printObject = true)
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

    /**
     * Extension point
     *
     * @param failedPayload
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
