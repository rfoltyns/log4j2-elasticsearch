package org.appenders.log4j2.elasticsearch.util;

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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

public class TestClock {

    private TestClock() {
        // static
    }

    public static Clock createTestClock(final long expectedTimestamp) {

        return new Clock() {

            @Override
            public ZoneId getZone() {
                return ZoneId.systemDefault();
            }

            @Override
            public Clock withZone(ZoneId zone) {
                throw new UnsupportedOperationException("Time zone testing with this Clock instance is not available");
            }

            @Override
            public Instant instant() {
                return Instant.ofEpochMilli(expectedTimestamp);
            }

        };

    }
}
