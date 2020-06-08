package org.appenders.core.logging;

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

        boolean throwOnNull = Boolean.valueOf(
                System.getProperty(THROW_ON_NULL_LOGGER, "false")
        );

        if (throwOnNull) {
            throw new IllegalStateException("Logger cannot be null. Set Logger instance with InternalLogging.setLogger()()");
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
