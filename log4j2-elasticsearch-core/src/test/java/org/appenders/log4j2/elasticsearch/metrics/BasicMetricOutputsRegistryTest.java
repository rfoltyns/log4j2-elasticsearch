package org.appenders.log4j2.elasticsearch.metrics;

import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BasicMetricOutputsRegistryTest {

    @Test
    public void addsGivenMetricOutput() {

        // given
        final MetricOutputsRegistry registry = new BasicMetricOutputsRegistry();

        // when
        registry.register(MetricOutputTest.dummy());

        // then
        assertEquals(1, registry.get(output -> true).size());

    }

    @Test
    public void removesGivenMetricOutput() {

        // given
        final MetricOutput metricOutput = spy(MetricOutputTest.dummy());
        final MetricOutputsRegistry registry = new BasicMetricOutputsRegistry(metricOutput);

        assertEquals(1, registry.get(output -> true).size());

        // when
        registry.deregister(metricOutput.getName());

        // then
        assertEquals(0, registry.get(output -> true).size());

    }

    @Test
    public void versionIsIncrementedOnMetricOutputAddition() {

        // given
        final MetricOutputsRegistry registry = new BasicMetricOutputsRegistry();
        final MetricOutput metricOutput = spy(MetricOutputTest.dummy());

        assertEquals(1, registry.version());

        // when
        registry.register(metricOutput);

        // then
        assertEquals(2, registry.version());

    }

    @Test
    public void versionIsIncrementedOnMetricOutputRemoval() {

        // given
        final MetricOutput metricOutput1 = spy(MetricOutputTest.dummy());
        final MetricOutput metricOutput2 = spy(MetricOutputTest.dummy());
        final MetricOutputsRegistry registry = createTestMetricOutputRegistry(metricOutput1, metricOutput2);

        assertEquals(1, registry.version());

        // when
        registry.deregister(metricOutput1.getName());
        registry.deregister(metricOutput2);

        // then
        assertEquals(3, registry.version());

    }

    protected BasicMetricOutputsRegistry createTestMetricOutputRegistry(final MetricOutput... metricOutputs) {
        return new BasicMetricOutputsRegistry(metricOutputs);
    }

    @Test
    public void versionIsNotIncrementedOnUnsuccessfulMetricOutputRemoval() {

        // given
        final MetricOutput metricOutput1 = spy(MetricOutputTest.dummy());
        final MetricOutput metricOutput2 = spy(MetricOutputTest.dummy());
        final MetricOutputsRegistry registry = createTestMetricOutputRegistry(metricOutput1, metricOutput2);

        registry.clear();

        assertEquals(2, registry.version());

        // when
        registry.deregister(metricOutput1.getName());
        registry.deregister(metricOutput2);

        // then
        assertEquals(2, registry.version());

    }

    @Test
    public void defaultVersionIsAlwaysEqualToOne() {

        // given
        final MetricOutputsRegistry registry1 = new BasicMetricOutputsRegistry();
        final MetricOutputsRegistry registry2 = new BasicMetricOutputsRegistry(MetricOutputTest.dummy());

        // when
        final long registry1version = registry1.version();
        final long registry2version = registry2.version();

        // then
        assertEquals(1, registry1version);
        assertEquals(1, registry2version);

        assertEquals(0, registry1.get(output -> true).size());
        assertEquals(1, registry2.get(output -> true).size());

    }

    @Test
    public void clearRemovesAllMetricOutputs() {

        // given
        final MetricOutputsRegistry registry = new BasicMetricOutputsRegistry(MetricOutputTest.dummy());
        registry.register(MetricOutputTest.dummy());

        assertEquals(2, registry.get(output -> true).size());

        // when
        registry.clear();

        // then
        assertEquals(3, registry.version());
        assertEquals(0, registry.get(output -> true).size());

    }

    // =========
    // LIFECYCLE
    // =========

    @Test
    public void lifecycleStartsOnlyOnce() {

        // given
        final MetricOutput metricOutput = spy(MetricOutputTest.dummy());
        when(metricOutput.accepts(any())).thenReturn(true);

        final LifeCycle lifeCycle = createLifeCycleTestObject(metricOutput);

        // when
        lifeCycle.start();
        lifeCycle.start();

        // then
        assertFalse(lifeCycle.isStopped());
        assertTrue(lifeCycle.isStarted());

        verify(LifeCycle.of(metricOutput)).start();

    }

    @Test
    public void lifecycleStopStopsOnlyOnce() {

        // given
        final MetricOutput metricOutput = spy(MetricOutputTest.dummy());
        when(metricOutput.accepts(any())).thenReturn(true);

        final LifeCycle lifeCycle = createLifeCycleTestObject(metricOutput);

        lifeCycle.start();
        assertTrue(lifeCycle.isStarted());

        // when
        lifeCycle.stop();
        lifeCycle.stop();

        // then
        assertTrue(lifeCycle.isStopped());
        assertFalse(lifeCycle.isStarted());

        verify(LifeCycle.of(metricOutput)).stop();

    }

    @Test
    public void lifecycleStart() {

        // given
        final MetricOutput metricOutput = spy(MetricOutputTest.dummy());
        when(metricOutput.accepts(any())).thenReturn(true);

        final LifeCycle lifeCycle = createLifeCycleTestObject(metricOutput);

        assertTrue(lifeCycle.isStopped());

        // when
        lifeCycle.start();
        lifeCycle.start();

        // then
        assertTrue(lifeCycle.isStarted());
        assertFalse(lifeCycle.isStopped());

        verify(LifeCycle.of(metricOutput)).start();

    }

    @Test
    public void lifecycleStop() {

        // given
        final MetricOutput metricOutput = spy(MetricOutputTest.dummy());
        when(metricOutput.accepts(any())).thenReturn(true);

        final LifeCycle lifeCycle = createLifeCycleTestObject(metricOutput);

        assertTrue(lifeCycle.isStopped());

        lifeCycle.start();
        assertTrue(lifeCycle.isStarted());

        // when
        lifeCycle.stop();
        lifeCycle.stop();

        // then
        assertFalse(lifeCycle.isStarted());
        assertTrue(lifeCycle.isStopped());

        verify(LifeCycle.of(metricOutput)).stop();

    }

    private LifeCycle createLifeCycleTestObject(final MetricOutput... metricOutputs) {
        return new BasicMetricOutputsRegistry(metricOutputs);
    }

}