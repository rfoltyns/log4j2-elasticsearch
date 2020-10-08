package org.appenders.log4j2.elasticsearch.hc;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BasicResponseTest {

    @Test
    public void isSucceededReturnFalseIfResponseCodeIsZero() {

        // given
        BasicResponse response = new BasicResponse().withResponseCode(0);

        // when
        boolean result = response.isSucceeded();

        // then
        assertFalse(result);

    }

    @Test
    public void isSucceededReturnFalseIfResponseCodeIs400() {

        // given
        BasicResponse response = new BasicResponse().withResponseCode(400);

        // when
        boolean result = response.isSucceeded();

        // then
        assertFalse(result);

    }

    @Test
    public void isSucceededReturnTrueIfResponseCodeIsLessThan400AndHigherThanZero() {

        // given
        BasicResponse response = new BasicResponse().withResponseCode(234);

        // when
        boolean result = response.isSucceeded();

        // then
        assertTrue(result);

    }

}