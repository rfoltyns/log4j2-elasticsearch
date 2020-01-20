package org.appenders.log4j2.elasticsearch.backoff;

/**
 * Allows to accumulate data and make decision based on it.
 *
 * @param <T> data type
 */
public interface BackoffPolicy<T> {

    String NAME = "BackoffPolicy";

    /**
     * SHOULD be used to make a decision on further steps ({@link #register(Object)} or {@link #deregister(Object)}
     *
     * @param data data
     * @return true, if policy should apply, false otherwise
     */
    boolean shouldApply(T data);

    /**
     * @param data data to collect before next decision
     */
    void register(T data);

    /**
     * @param data data to remove before next decision
     */
    void deregister(T data);

}
