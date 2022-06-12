package org.appenders.log4j2.elasticsearch;

import org.jctools.queues.MpmcUnboundedXaddArrayQueue;
import org.jctools.queues.MpscUnboundedArrayQueue;
import org.jctools.queues.SpscUnboundedArrayQueue;

public class JCToolsFactory {

    public <T> QueueFactory.Factory<T> create(final String queueClassName) {

        if (queueClassName.endsWith("MpmcUnboundedXaddArrayQueue")) {
            return MpmcUnboundedXaddArrayQueue::new;
        }
        if (queueClassName.endsWith("MpscUnboundedArrayQueue")) {
            return MpscUnboundedArrayQueue::new;
        }
        if (queueClassName.endsWith("SpscUnboundedArrayQueue")) {
            return SpscUnboundedArrayQueue::new;
        }

        throw new IllegalArgumentException("Class not supported: " + queueClassName);

    }

}
