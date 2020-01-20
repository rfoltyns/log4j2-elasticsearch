package org.appenders.log4j2.elasticsearch.hc.backoff;

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
