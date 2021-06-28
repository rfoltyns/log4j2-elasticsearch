package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2019 Rafal Foltynski
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


import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class Log4j2LookupTest {

    @Test
    public void startsSuccessfully() {

        // given
        StrSubstitutor strSubstitutor = createDefaultTestStrSubstitutor();

        // when
        Log4j2Lookup result = createDefaultTestLog4j2Lookup(strSubstitutor);

        // then
        assertNotNull(result);

    }

    @Test
    public void resolvesDynamicPropertyOnEachCall() {

        // given
        String expectedValue = UUID.randomUUID().toString();
        VirtualProperty virtualProperty = VirtualPropertyTest.createDefaultVirtualPropertyBuilder()
                .withValue(expectedValue)
                .withDynamic(true)
                .build();

        StrSubstitutor strSubstitutor = spy(createDefaultTestStrSubstitutor());
        Log4j2Lookup lookup = createDefaultTestLog4j2Lookup(strSubstitutor);
        LogEvent event1 = mock(LogEvent.class);
        LogEvent event2 = mock(LogEvent.class);

        // when
        lookup.resolve(virtualProperty, event1);
        lookup.resolve(virtualProperty, event2);

        // then
        verify(strSubstitutor).replace(event1, expectedValue);
        verify(strSubstitutor).replace(event2, expectedValue);
    }

    @Test
    public void resolvesDynamicPropertyDelegatesToStringBasedApi() {

        // given
        String expectedValue = UUID.randomUUID().toString();
        VirtualProperty virtualProperty = VirtualPropertyTest.createDefaultVirtualPropertyBuilder()
                .withValue(expectedValue)
                .withDynamic(true)
                .build();

        Log4j2Lookup lookup = spy(createDefaultTestLog4j2Lookup(createDefaultTestStrSubstitutor()));

        // when
        lookup.resolve(virtualProperty, null);

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        verify(lookup, times(1)).resolve(captor.capture(), any());

        assertEquals(expectedValue, captor.getValue());

    }

    @Test
    public void doesNotResolveNonDynamicVirtualProperties() {

        // given
        VirtualProperty virtualProperty = VirtualPropertyTest.createDefaultVirtualPropertyBuilder()
                .withDynamic(false)
                .build();

        StrSubstitutor strSubstitutor = spy(createDefaultTestStrSubstitutor());
        Log4j2Lookup lookup = createDefaultTestLog4j2Lookup(strSubstitutor);

        // when
        lookup.resolve(virtualProperty);

        // then
        verifyNoInteractions(strSubstitutor);

    }

    private Log4j2Lookup createDefaultTestLog4j2Lookup(StrSubstitutor strSubstitutor) {
        return new Log4j2Lookup(strSubstitutor);
    }

    private StrSubstitutor createDefaultTestStrSubstitutor() {
        return LoggerContext.getContext(false).getConfiguration().getStrSubstitutor();
    }

}
