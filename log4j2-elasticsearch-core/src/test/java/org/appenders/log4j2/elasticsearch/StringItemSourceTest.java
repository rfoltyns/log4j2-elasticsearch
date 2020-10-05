package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2018 Rafal Foltynski
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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringItemSourceTest {

    @Test
    public void doesntChangeTheSource() {

        // given
        String expected = "expectedSource";

        // when
        ItemSource<String> itemSource = createTestStringItemSource(expected);

        // then
        assertEquals(expected, itemSource.getSource());
        assertEquals(expected, itemSource.toString());

    }

    @Test
    public void releaseHasNoSideEffect() {

        // given
        String expected = "expectedSource";
        ItemSource<String> itemSource = createTestStringItemSource(expected);

        // when
        itemSource.release();

        // then
        assertEquals(expected, itemSource.getSource());

    }

    public static StringItemSource createTestStringItemSource(String expected) {
        return new StringItemSource(expected);
    }

}
