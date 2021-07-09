package org.appenders.log4j2.elasticsearch.smoke;

import org.appenders.core.logging.Logger;

public class Log4j2Wrapper implements Logger {

    private final org.apache.logging.log4j.Logger logger;

    public Log4j2Wrapper(org.apache.logging.log4j.Logger logger) {
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
