package org.appenders.core.logging;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2020 Rafal Foltynski
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

/**
 * Allows to set an arbitrary logging subsystem wrapped with {@link Logger} interface.
 *
 * <p>By default, if no {@link Logger} instance is set, {@link Log4j2StatusLoggerWrapper} instance will be created.
 * This behaviour can be changed (mainly to detect {@link InternalLogging} setup issues) by
 * setting <i>{@link #THROW_ON_NULL_LOGGER}</i> property to <i>true</i>.
 */
public final class InternalLogging {

    static final String THROW_ON_NULL_LOGGER = "appenders.internalLogging.throwOnNull";

    private static Logger logger;

    private InternalLogging() {
        // noop
    }

    /**
     * @return By default, if no {@link Logger} instance is set, {@link Log4j2StatusLoggerWrapper} instance will be returned.
     * Otherwise, an instance of {@link Logger} set with {@link #setLogger(Logger)} will be returned.
     */
    public static Logger getLogger() {

        if (logger != null) {
            return logger;
        }

        boolean throwOnNull = Boolean.parseBoolean(
                System.getProperty(THROW_ON_NULL_LOGGER, "false")
        );

        if (throwOnNull) {
            throw new IllegalStateException("Logger cannot be null. Set Logger instance with InternalLogging.setLogger()");
        } else {
            // TODO: should fallback to SLF4J wrapper (eventually, in future releases)
            logger = new Log4j2StatusLoggerWrapper();
        }

        return logger;
    }

    /**
     * Allows to set an arbitrary {@link Logger}
     *
     * @param logger {@link Logger} instance returned by subsequent {@link #getLogger()} calls
     */
    public static void setLogger(Logger logger) {
        InternalLogging.logger = logger;
    }

}
