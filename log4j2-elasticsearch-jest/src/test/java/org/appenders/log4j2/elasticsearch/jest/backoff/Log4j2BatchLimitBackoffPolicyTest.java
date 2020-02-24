package org.appenders.log4j2.elasticsearch.jest.backoff;

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

import org.apache.logging.log4j.core.config.ConfigurationException;
import org.appenders.log4j2.elasticsearch.backoff.BackoffPolicy;
import org.appenders.log4j2.elasticsearch.backoff.BatchLimitBackoffPolicy;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class Log4j2BatchLimitBackoffPolicyTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void builderBuildsSuccessfully() {

        // given
        Log4j2BatchLimitBackoffPolicy.Builder builder = Log4j2BatchLimitBackoffPolicy.newBuilder();

        // when
        BackoffPolicy policy = builder.build();

        // then
        Assert.assertNotNull(policy);

    }

    @Test
    public void builderThrowsIfMaxBatchesInFlightEqualsZero() {

        // given
        Log4j2BatchLimitBackoffPolicy.Builder builder = Log4j2BatchLimitBackoffPolicy.newBuilder()
                .withMaxBatchesInFlight(0);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("maxBatchesInFlight must be higher than 0 for " +
                BatchLimitBackoffPolicy.class.getSimpleName());

        // when
        builder.build();

    }

    @Test
    public void builderThrowsIfMaxBatchesInFlightLowerThanZero() {

        // given
        Log4j2BatchLimitBackoffPolicy.Builder builder = Log4j2BatchLimitBackoffPolicy.newBuilder()
                .withMaxBatchesInFlight(-1);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("maxBatchesInFlight must be higher than 0 for " +
                BatchLimitBackoffPolicy.class.getSimpleName());

        // when
        builder.build();

    }
}
