package org.appenders.core.logging;

import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.status.StatusLogger;

class Log4j2StatusLoggerWrapper implements Logger {

    private final AbstractLogger logger;

    Log4j2StatusLoggerWrapper() {
        this.logger = StatusLogger.getLogger();
    }

    Log4j2StatusLoggerWrapper(AbstractLogger logger) {
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
