package org.appenders.core.logging;

public interface Logger {

    default void error(String messageFormat, Object... parameters) {
        throw new UnsupportedOperationException("Not implemented");
    }

    default void warn(String messageFormat, Object...parameters) {
        throw new UnsupportedOperationException("Not implemented");
    }

    default void info(String messageFormat, Object... parameters) {
        throw new UnsupportedOperationException("Not implemented");
    }

    default void debug(String messageFormat, Object...parameters) {
        throw new UnsupportedOperationException("Not implemented");
    }

    default void trace(String messageFormat, Object...parameters) {
        throw new UnsupportedOperationException("Not implemented");
    }

}
