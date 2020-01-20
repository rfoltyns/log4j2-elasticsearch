package org.appenders.log4j2.elasticsearch.backoff;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class NoopBackoffPolicyTest {

    private Random random = new Random();

    @Test
    public void buildsSuccessfully() {

        // when
        BackoffPolicy<Object> policy = new NoopBackoffPolicy<>();

        // then
        assertNotNull(policy);

    }

    @Test
    public void registerHasNoEffect() {

        // given
        BackoffPolicy<Object> policy = new NoopBackoffPolicy<>();
        assertFalse(policy.shouldApply(null));

        // when
        for (int i = 0; i < random.nextInt(100000); i++) {
            policy.register(null);
        }

        // then
        assertFalse(policy.shouldApply(null));

    }


    @Test
    public void deregisterHasNoEffect() {

        // given
        BackoffPolicy<Object> policy = new NoopBackoffPolicy<>();
        assertFalse(policy.shouldApply(null));

        // when
        for (int i = 0; i < random.nextInt(100000); i++) {
            policy.deregister(null);
        }

        // then
        assertFalse(policy.shouldApply(null));

    }

}
