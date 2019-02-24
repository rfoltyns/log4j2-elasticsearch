package org.appenders.log4j2.elasticsearch.mock;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.atomic.AtomicBoolean;

public class LifecycleTestHelper {

    public static Answer<Boolean> trueOnlyOnce() {
        return new Answer<Boolean>() {
            AtomicBoolean state = new AtomicBoolean(true);

            @Override
            public Boolean answer(InvocationOnMock invocation) {
                return state.compareAndSet(true, false);
            }
        };
    }

    public static Answer<Boolean> falseOnlyOnce() {
        return new Answer<Boolean>() {
            AtomicBoolean state = new AtomicBoolean();

            @Override
            public Boolean answer(InvocationOnMock invocation) {
                return !state.compareAndSet(false, true);
            }
        };
    }

}
