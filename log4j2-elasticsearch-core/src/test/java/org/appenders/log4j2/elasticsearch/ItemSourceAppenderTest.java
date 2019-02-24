package org.appenders.log4j2.elasticsearch;

import org.appenders.log4j2.elasticsearch.mock.LifecycleTestHelper;
import org.junit.Test;

import static org.appenders.log4j2.elasticsearch.mock.LifecycleTestHelper.falseOnlyOnce;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ItemSourceAppenderTest {

    @Test
    public void lifecycleStart() {

        // given
        LifeCycle lifeCycle = createLifeCycleTestObject();

        assertTrue(lifeCycle.isStopped());

        // when
        lifeCycle.start();

        // then
        assertFalse(lifeCycle.isStopped());
        assertTrue(lifeCycle.isStarted());

    }

    @Test
    public void lifecycleStop() {

        // given
        LifeCycle lifeCycle = createLifeCycleTestObject();

        assertTrue(lifeCycle.isStopped());

        lifeCycle.start();
        assertTrue(lifeCycle.isStarted());

        // when
        lifeCycle.stop();

        // then
        assertFalse(lifeCycle.isStarted());
        assertTrue(lifeCycle.isStopped());

    }

    @Test
    public void lifecycleStartStartsBatchDelivery() {

        // given
        BatchDelivery batchDelivery = mock(BatchDelivery.class);
        ItemAppender itemAppender = new ItemSourceAppender(batchDelivery, logEvent -> null);

        // when
        itemAppender.start();

        // then
        verify(batchDelivery).start();

    }

    @Test
    public void lifecycleStopStopsBatchDeliveryIfStarted() {

        // given
        BatchDelivery batchDelivery = mock(BatchDelivery.class);
        when(batchDelivery.isStarted()).thenReturn(true);

        ItemAppender itemAppender = new ItemSourceAppender(batchDelivery, logEvent -> null);

        // when
        itemAppender.stop();

        // then
        verify(batchDelivery).stop();

    }

    @Test
    public void lifecycleStopStopsBatchDeliveryOnlyOnce() {

        // given
        BatchDelivery batchDelivery = mock(BatchDelivery.class);
        when(batchDelivery.isStopped()).thenAnswer(falseOnlyOnce());

        ItemAppender itemAppender = new ItemSourceAppender(batchDelivery, logEvent -> null);

        // when
        itemAppender.stop();
        itemAppender.stop();

        // then
        verify(batchDelivery).stop();

    }

    private LifeCycle createLifeCycleTestObject() {
        BatchDelivery batchDelivery = mock(BatchDelivery.class);
        when(batchDelivery.isStarted()).thenReturn(false);
        return new ItemSourceAppender(batchDelivery, logEvent -> null);
    }

}
