package org.appenders.log4j2.elasticsearch;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * Allows to translate client-specific responses to {@link Result}.
 *
 * @param <T> response type
 */
public interface SetupCallback<T> {

    Result onResponse(T response);

    default Result onException(Exception e) {

        getLogger().error("{}: {} {}",
                getClass().getSimpleName(), e.getClass().getSimpleName(), e.getMessage());

        return Result.FAILURE;

    }

}
