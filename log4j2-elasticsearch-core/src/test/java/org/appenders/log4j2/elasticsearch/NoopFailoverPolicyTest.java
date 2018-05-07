package org.appenders.log4j2.elasticsearch;

import org.junit.Test;
import static org.junit.Assert.assertNotNull;

public class NoopFailoverPolicyTest {

    @Test
    public void minimalBuilderTest() {

        // given
        NoopFailoverPolicy.Builder builder = NoopFailoverPolicy.newBuilder();

        // when
        FailoverPolicy failoverPolicy = builder.build();

        // then
        assertNotNull(failoverPolicy);
    }

    @Test
    public void deliverReturnsImmediately() {

        // given
        NoopFailoverPolicy.Builder builder = NoopFailoverPolicy.newBuilder();
        FailoverPolicy failoverPolicy = builder.build();

        // when
        failoverPolicy.deliver(null);

    }
}
