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

import java.util.Random;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class NoopBackoffPolicyTest {

    private final Random random = new Random();

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
