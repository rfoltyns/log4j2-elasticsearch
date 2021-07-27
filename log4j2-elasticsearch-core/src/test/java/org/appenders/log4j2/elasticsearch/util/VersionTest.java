package org.appenders.log4j2.elasticsearch.util;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2021 Rafal Foltynski
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

import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VersionTest {

    @Test
    public void throwsIfVersionIsNull() {

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> VersionUtil.parse(null));

        // then
        assertThat(exception.getMessage(), IsEqual.equalTo("Version cannot be blank"));

    }

    @Test
    public void throwsIfVersionIsEmpty() {

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> VersionUtil.parse(""));

        // then
        assertThat(exception.getMessage(), IsEqual.equalTo("Version cannot be blank"));

    }

    @Test
    public void throwsIfVersionIsBlank() {

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> VersionUtil.parse(" "));

        // then
        assertThat(exception.getMessage(), IsEqual.equalTo("Version cannot be blank"));

    }

    @Test
    public void throwsIfVersionPartIsNegative() {

        // given
        final Version version = VersionUtil.parse("7.10.2");
        final Version other = VersionUtil.parse("7.-1.2");

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> version.lowerThan(other));

        // then
        assertThat(exception.getMessage(), IsEqual.equalTo("Version part cannot be negative: -1"));

    }

    @Test
    public void throwsIfVersionPartIsNotANumber() {

        // given
        final Version version = VersionUtil.parse("7.10.2");
        final Version other = VersionUtil.parse("7.a.2");

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> version.lowerThan(other));

        // then
        assertThat(exception.getMessage(), IsEqual.equalTo("Not a number: a"));

    }

    @Test
    public void throwsIfOtherVersionPartIsNegative() {

        // given
        final Version version = VersionUtil.parse("7.10.2");
        final Version other = VersionUtil.parse("7.-1.2");

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> version.higherThan(other));

        // then
        assertThat(exception.getMessage(), IsEqual.equalTo("Version part cannot be negative: -1"));

    }

    @Test
    public void throwsIfOtherVersionPartIsNotANumber() {

        // given
        final Version version = VersionUtil.parse("7.10.2");
        final Version other = VersionUtil.parse("7.a.2");

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> version.higherThan(other));

        // then
        assertThat(exception.getMessage(), IsEqual.equalTo("Not a number: a"));

    }

    @Test
    public void canReturnMajorVersion() {

        // given
        final Version version = VersionUtil.parse("7.10.2");

        // when
        final int result = version.major();

        // then
        assertEquals(7, result);

    }

    @Test
    public void canCompareWithLowerVersions() {

        // given
        final Version version = VersionUtil.parse("7.10.2");

        // when
        final boolean higherThan = version.higherThan("7.9.1");
        final boolean lowerThan = version.lowerThan("7.9.1");

        // then
        assertTrue(higherThan);
        assertFalse(lowerThan);

    }

    @Test
    public void canCompareWithHigherVersions() {

        // given
        final Version version = VersionUtil.parse("7.9.1");

        // when
        final boolean higherThan = version.higherThan("7.10.2");
        final boolean lowerThan = version.lowerThan("7.10.2");

        // then
        assertFalse(higherThan);
        assertTrue(lowerThan);

    }

    @Test
    public void canCompareWithEqualVersions() {

        // given
        final Version version = VersionUtil.parse("7.10.2");

        // when
        final boolean higherThan = version.higherThan("7.10.2");
        final boolean lowerThan = version.lowerThan("7.10.2");

        // then
        assertFalse(higherThan);
        assertFalse(lowerThan);

    }

    @Test
    public void canPrintVersion() {

        // then
        final Version version = VersionUtil.parse("7.10.2");

        // when
        final String result = version.toString();

        // then
        assertEquals("7.10.2", result);

    }

}
