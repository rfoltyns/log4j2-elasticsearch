package org.appenders.log4j2.elasticsearch.jcstress;

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

import org.appenders.log4j2.elasticsearch.RollingMillisFormatter;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.LLL_Result;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

@JCStressTest
@Outcome.Outcomes(value = {
        @Outcome(id="jcstress-2017-12-20-23.59, jcstress-2017-12-21-00.00, jcstress-2017-12-21-00.01", expect = Expect.ACCEPTABLE, desc="OK"),
})
@State
public class RollingMillisFormatterTest {

    private static final String DATE_PATTERN_WITH_MINUTES = "yyyy-MM-dd-HH.mm";
    private static final String TEST_INDEX_NAME = "jcstress";
    private static final ZoneId TEST_TIME_ZONE = ZoneId.of(RollingMillisFormatter.Builder.DEFAULT_TIME_ZONE);

    private static final long INITIAL_TIMESTAMP = getTestTimeInMillis();
    private static final long MINUTE = TimeUnit.MINUTES.toMillis(1);
    private static final long ONE_MINUTE_LATER = INITIAL_TIMESTAMP + MINUTE;
    private static final long TWO_MINUTES_LATER = INITIAL_TIMESTAMP + MINUTE * 2;

    private final RollingMillisFormatter formatter = createFormatter();

    @Actor
    public void minutes1(LLL_Result r) {
        r.r1 = formatter.format(INITIAL_TIMESTAMP);
    }

    @Actor
    public void minutes2(LLL_Result r) {
        r.r2 = formatter.format(ONE_MINUTE_LATER);
    }

    @Actor
    public void minutes3(LLL_Result r) {
        r.r3 = formatter.format(TWO_MINUTES_LATER);
    }

    private static long getTestTimeInMillis() {
        return LocalDateTime.of(2017, 12, 20, 23, 59, 0, 0)
                .atZone(ZoneId.of(RollingMillisFormatter.Builder.DEFAULT_TIME_ZONE))
                .toInstant().toEpochMilli();
    }

    private static RollingMillisFormatter createFormatter() {

        return new RollingMillisFormatter.Builder()
                .withPrefix(TEST_INDEX_NAME)
                .withSeparator("-")
                .withPattern(RollingMillisFormatterTest.DATE_PATTERN_WITH_MINUTES)
                .withTimeZone(TEST_TIME_ZONE.getId())
                .withInitialTimestamp(getTestTimeInMillis())
                .build();

    }

}
