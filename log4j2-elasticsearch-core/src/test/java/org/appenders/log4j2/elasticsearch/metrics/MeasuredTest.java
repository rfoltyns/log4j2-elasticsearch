package org.appenders.log4j2.elasticsearch.metrics;

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

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

public class MeasuredTest {

    @Test
    public void returnsMeasuredInstanceWhenGivenMeasuredInstance() {

        // given
        final Object expected = new MeasuredTestType();

        // when
        final Measured result = Measured.of(expected);

        // then
        assertSame(expected, result);
        assertNotSame(Measured.NOOP, result);

    }

    @Test
    public void returnsNoopWhenGivenNotMeasuredInstance() {

        // given
        final Object notExpected = new NotMeasuredTestType();

        // when
        final Measured result = Measured.of(notExpected);

        // then
        assertNotSame(notExpected, result);
        assertSame(Measured.NOOP, result);

    }

    @Test
    public void noopRegisterHasNoSideEffects() {

        // given
        final Object notExpected = new NotMeasuredTestType();
        final Measured measured = Measured.of(notExpected);
        final MetricsRegistry registry = mock(MetricsRegistry.class);

        // when
        measured.register(registry);

        // then
        verifyNoInteractions(registry);

    }

    private static class MeasuredTestType implements Measured {

        @Override
        public void register(MetricsRegistry registry) {

        }
    }

    private static class NotMeasuredTestType {
    }

}
