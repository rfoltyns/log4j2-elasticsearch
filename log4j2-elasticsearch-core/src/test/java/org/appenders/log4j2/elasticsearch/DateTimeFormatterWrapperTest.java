package org.appenders.log4j2.elasticsearch;

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

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DateTimeFormatterWrapperTest {

    @Test
    public void formatsMillisWithGivenFormatter() {

        // given
        final long millis = 1646511741015L;
        final String expectedFormattedMillis = "2022-03-05-20-22-21-015";
        final DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd-HH-mm-ss-SSS").toFormatter().withZone(ZoneId.systemDefault());

        // when
        final DateTimeFormatterWrapper formatter = new DateTimeFormatterWrapper(dateTimeFormatter, 32);
        final String result = formatter.format(millis);

        // then
        assertEquals(expectedFormattedMillis, result);

    }

}
