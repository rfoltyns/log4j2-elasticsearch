package org.appenders.log4j2.elasticsearch.metrics;

import java.util.Set;
import java.util.function.Predicate;

/**
 * {@link MetricOutput}s store.
 */
public interface MetricOutputsRegistry {

    /**
     * Latest version of this instance. MUST change if output is added or removed
     *
     * @return latest version
     */
    long version();

    /**
     * @param predicate output filter
     * @return matching outputs
     */
    Set<MetricOutput> get(Predicate<MetricOutput> predicate);

    /**
     * SHOULD register given {@link MetricOutput}. On success, {@link #version()} MUST be updated.
     *
     * @param metricOutput output to be registered
     */
    void register(MetricOutput metricOutput);

    /**
     * SHOULD deregister {@link MetricOutput} by name. See {@link MetricOutput#getName()}. On success, {@link #version()} MUST be updated
     *
     * @param name output name
     */
    void deregister(String name);

    /**
     * SHOULD deregister given {@link MetricOutput}. On success, {@link #version()} MUST be updated
     *
     * @param metricOutput output to be deregistered
     */
    void deregister(MetricOutput metricOutput);

    /**
     * SHOULD remove all known outputs
     */
    void clear();

}
