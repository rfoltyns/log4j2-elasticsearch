package org.appenders.log4j2.elasticsearch;

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

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ValueResolverTest {

    @Test
    public void noopResolverDoesNotChangeGivenValue() {

        // given
        String expected = UUID.randomUUID().toString();

        // when
        String resolved = ValueResolver.NO_OP.resolve(expected);

        // then
        assertSame(expected, resolved);

    }

    @Test
    public void noopResolverReturnsCurrentVirtualPropertyValue() {

        // given
        VirtualProperty property = mock(VirtualProperty.class);
        String expected = UUID.randomUUID().toString();
        when(property.getValue()).thenReturn(expected);

        // when
        String resolved = ValueResolver.NO_OP.resolve(property);

        // then
        assertSame(expected, resolved);

    }

}
