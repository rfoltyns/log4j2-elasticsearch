package org.appenders.log4j2.elasticsearch.ahc.backoff;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2022 Rafal Foltynski
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
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class Log4j2BatchLimitBackoffPolicyTest {

    @Test
    public void builderBuildsSuccessfully() {

        // given
        final Log4j2BatchLimitBackoffPolicy.Builder builder = Log4j2BatchLimitBackoffPolicy.newBuilder();

        // when
        final BackoffPolicy policy = builder.build();

        // then
        assertNotNull(policy);

    }

    @Test
    public void builderThrowsIfMaxBatchesInFlightEqualsZero() {

        // given
        final Log4j2BatchLimitBackoffPolicy.Builder builder = Log4j2BatchLimitBackoffPolicy.newBuilder()
                .withMaxBatchesInFlight(0);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), equalTo("maxBatchesInFlight must be higher than 0 for " +
                BatchLimitBackoffPolicy.class.getSimpleName()));

    }

    @Test
    public void builderThrowsIfMaxBatchesInFlightLowerThanZero() {

        // given
        final Log4j2BatchLimitBackoffPolicy.Builder builder = Log4j2BatchLimitBackoffPolicy.newBuilder()
                .withMaxBatchesInFlight(-1);

        // when
        final ConfigurationException exception = assertThrows(ConfigurationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), equalTo("maxBatchesInFlight must be higher than 0 for " +
                BatchLimitBackoffPolicy.class.getSimpleName()));

    }
}
