package org.appenders.log4j2.elasticsearch.ahc;

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BasicResponseTest {

    @Test
    public void isSucceededReturnFalseIfResponseCodeIsZero() {

        // given
        final BasicResponse response = new BasicResponse().withResponseCode(0);

        // when
        final boolean result = response.isSucceeded();

        // then
        assertFalse(result);

    }

    @Test
    public void isSucceededReturnFalseIfResponseCodeIs400() {

        // given
        final BasicResponse response = new BasicResponse().withResponseCode(400);

        // when
        final boolean result = response.isSucceeded();

        // then
        assertFalse(result);

    }

    @Test
    public void isSucceededReturnTrueIfResponseCodeIsLessThan400AndHigherThanZero() {

        // given
        final BasicResponse response = new BasicResponse().withResponseCode(234);

        // when
        final boolean result = response.isSucceeded();

        // then
        assertTrue(result);

    }

}
