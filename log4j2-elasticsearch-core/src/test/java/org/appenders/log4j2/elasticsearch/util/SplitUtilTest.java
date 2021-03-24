package org.appenders.log4j2.elasticsearch.util;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2020 Rafal Foltynski
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

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SplitUtilTest {

    @Test
    public void splitsBySemicolonByDefault() {

        // given
        String part1 = UUID.randomUUID().toString();
        String part2 = UUID.randomUUID().toString();

        String input = part1 + ";" + part2;

        // when
        List<String> result = SplitUtil.split(input);

        // then
        assertEquals(part1, result.get(0));
        assertEquals(part2, result.get(1));

    }

    @Test
    public void splitsByGivenSeparator() {

        // given
        String part1 = UUID.randomUUID().toString();
        String part2 = UUID.randomUUID().toString();
        String separator = UUID.randomUUID().toString();

        String input = part1 + separator + part2;

        // when
        List<String> result = SplitUtil.split(input, separator);

        // then
        assertEquals(part1, result.get(0));
        assertEquals(part2, result.get(1));

    }

    @Test
    public void returnsListWithWholeInputIfSeparatorNotFound() {

        // given
        String part1 = UUID.randomUUID().toString();
        String part2 = UUID.randomUUID().toString();
        String separator = UUID.randomUUID().toString();

        String input = part1 + part2;

        // when
        List<String> result = SplitUtil.split(input, separator);

        // then
        assertEquals(1 ,result.size());
        assertEquals(input, result.get(0));

    }

    @Test
    public void returnsListWithPartIfStartsWithSeparator() {

        // given
        String part1 = UUID.randomUUID().toString();
        String separator = UUID.randomUUID().toString();

        String input = separator + part1;

        // when
        List<String> result = SplitUtil.split(input, separator);

        // then
        assertEquals(1 ,result.size());
        assertEquals(part1, result.get(0));

    }

    @Test
    public void returnsListWithPartIfEndsWithSeparator() {

        // given
        String part1 = UUID.randomUUID().toString();
        String separator = UUID.randomUUID().toString();

        String input = part1 + separator;

        // when
        List<String> result = SplitUtil.split(input, separator);

        // then
        assertEquals(1 ,result.size());
        assertEquals(part1, result.get(0));

    }

    @Test
    public void returnsListWithNonBlankParts() {

        // given
        String part1 = UUID.randomUUID().toString();
        String separator = UUID.randomUUID().toString();

        String input = " " + separator + part1 + separator + " ";

        // when
        List<String> result = SplitUtil.split(input, separator);

        // then
        assertEquals(1 ,result.size());
        assertEquals(part1, result.get(0));

    }

    @Test
    public void returnsEmptyListIfInputIsBlank() {

        // given
        String separator = UUID.randomUUID().toString();

        String input = " ";

        // when
        List<String> result = SplitUtil.split(input, separator);

        // then
        assertEquals(0 ,result.size());

    }

    @Test
    public void returnsEmptyListIfInputIsNull() {

        // given
        String separator = UUID.randomUUID().toString();

        // when
        List<String> result = SplitUtil.split(null, separator);

        // then
        assertEquals(0 ,result.size());

    }

}
