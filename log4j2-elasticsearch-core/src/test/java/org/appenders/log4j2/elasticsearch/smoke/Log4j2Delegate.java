package org.appenders.log4j2.elasticsearch.smoke;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2021 Rafal Foltynski
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

import org.apache.logging.log4j.LogManager;
import org.appenders.core.logging.Logger;

public class Log4j2Delegate implements Logger {

    private final org.apache.logging.log4j.Logger logger;

    public Log4j2Delegate(final String loggerName) {
        this.logger = LogManager.getLogger(loggerName);
    }

    public Log4j2Delegate(final org.apache.logging.log4j.Logger logger) {
        this.logger = logger;
    }

    @Override
    public void error(String messageFormat, Object... parameters) {
        logger.error(messageFormat, parameters);
    }

    @Override
    public void warn(String messageFormat, Object... parameters) {
        logger.warn(messageFormat, parameters);
    }

    @Override
    public void info(String messageFormat, Object... parameters) {
        logger.info(messageFormat, parameters);
    }

    @Override
    public void debug(String messageFormat, Object... parameters) {
        logger.debug(messageFormat, parameters);
    }

    @Override
    public void trace(String messageFormat, Object... parameters) {
        logger.trace(messageFormat, parameters);
    }
}
