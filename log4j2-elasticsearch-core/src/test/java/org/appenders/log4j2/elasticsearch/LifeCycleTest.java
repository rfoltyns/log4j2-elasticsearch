package org.appenders.log4j2.elasticsearch;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.spy;

public class LifeCycleTest {

    @Test
    public void returnsLifeCycleOfGivenObjectIfAvailable() {

        // given
        TestObject expected = createDefaultTestLifecycle();

        // when
        LifeCycle lifeCycle = LifeCycle.of(expected);

        // then
        assertEquals(expected, lifeCycle);

    }

    @Test
    public void returnsNoopLifeCycleIfNonLifeCycleObject() {

        // given
        Object expected = new Object();

        // when
        LifeCycle lifeCycle = LifeCycle.of(expected);

        // then
        assertNotEquals(expected, lifeCycle);
        assertSame(LifeCycle.NOOP, lifeCycle);

    }

    @Test
    public void stopDelegatesToParametrizedStop() {

        // given
        LifeCycle lifeCycle = spy(createDefaultTestLifecycle());

        // when
        lifeCycle.stop();

        // then
        Mockito.verify(lifeCycle).stop(anyLong(), anyBoolean());
    }

    public static TestObject createDefaultTestLifecycle() {
        return new TestObject();
    }

    private static class TestObject implements LifeCycle {

        @Override
        public void start() {

        }

        @Override
        public boolean isStarted() {
            return false;
        }

        @Override
        public boolean isStopped() {
            return false;
        }

    }

}
