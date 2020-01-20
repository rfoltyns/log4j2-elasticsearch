package org.appenders.log4j2.elasticsearch.backoff;

/**
 * Default implementation. SHOULD have no impact on caller's behaviour
 *
 * @param <T> client-specific type
 */
public class NoopBackoffPolicy<T> implements BackoffPolicy<T> {

    /**
     * @param request not used
     * @return false
     */
    @Override
    public final boolean shouldApply(T request) {
        return false;
    }

    @Override
    public void register(T request) {
        // noop
    }

    @Override
    public void deregister(Object result) {
        // noop
    }

}
