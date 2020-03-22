package org.appenders.log4j2.elasticsearch.backoff;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2019 - 2020 Rafal Foltynski
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BatchLimitBackoffPolicyTest {

    public static final int DEFAULT_TEST_MAX_BATCHES_IN_FLIGHT = 1;

    @Test
    public void buildsSuccessfully() {

        // when
        BackoffPolicy<Object> policy = new BatchLimitBackoffPolicy<>(DEFAULT_TEST_MAX_BATCHES_IN_FLIGHT);

        // then
        assertNotNull(policy);

    }

    @Test
    public void registerIncrementsCurrentCount() {

        // given
        BackoffPolicy<Object> policy = new BatchLimitBackoffPolicy<>(1);
        assertFalse(policy.shouldApply(null));

        // when
        policy.register(null);

        // then
        assertTrue(policy.shouldApply(null));

    }

    @Test
    public void deregisterDecrementsCurrentCount() {

        // given
        BackoffPolicy<Object> policy = new BatchLimitBackoffPolicy<>(1);
        assertFalse(policy.shouldApply(null));

        policy.register(null);
        assertTrue(policy.shouldApply(null));

        // when
        policy.deregister(null);

        // then
        assertFalse(policy.shouldApply(null));

    }

    @Test
    public void shouldApplyReturnsFalseIfBatchesInFlightLowerThanConfigured() {

        // given
        BackoffPolicy<Object> policy = new BatchLimitBackoffPolicy<>(1);

        // when
        boolean result = policy.shouldApply(null);

        // then
        assertFalse(result);

    }

    @Test
    public void shouldApplyReturnsTrueIfBatchesInFlightEqualToConfigured() {

        // given
        BackoffPolicy<Object> policy = new BatchLimitBackoffPolicy<>(1);
        assertFalse(policy.shouldApply(null));

        // when
        policy.register(null);

        // then
        assertTrue(policy.shouldApply(null));

    }

    @Test
    public void shouldApplyReturnsTrueIfBatchesInFlightHigherThanConfigured() {

        // given
        BackoffPolicy<Object> policy = new BatchLimitBackoffPolicy<>(1);
        assertFalse(policy.shouldApply(null));

        // when
        policy.register(null);
        policy.register(null);

        // then
        assertTrue(policy.shouldApply(null));

    }

}
