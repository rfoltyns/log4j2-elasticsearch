package org.appenders.log4j2.elasticsearch;

import org.appenders.core.logging.Logger;

public class SysOutLogger implements Logger {

    @Override
    public void error(String messageFormat, Object... parameters) {
        log(messageFormat, parameters);
    }

    @Override
    public void warn(String messageFormat, Object... parameters) {
        log(messageFormat, parameters);
    }

    @Override
    public void info(String messageFormat, Object... parameters) {
        log(messageFormat, parameters);
    }

    @Override
    public void debug(String messageFormat, Object... parameters) {
        log(messageFormat, parameters);
    }

    @Override
    public void trace(String messageFormat, Object... parameters) {
        log(messageFormat, parameters);
    }

    private void log(String messageFormat, Object[] parameters) {
        System.out.printf((messageFormat.replace("{}", "%s")) + "%n", parameters);
        System.out.println();
    }

}