package org.appenders.log4j2.elasticsearch.bulkprocessor;

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

import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.appenders.core.logging.InternalLoggingTest.mockTestLogger;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class LoggingActionListenerTest {

    @AfterEach
    public void tearDown() {
        InternalLogging.setLogger(null);
    }

    @Test
    public void logsOnResponse() {

        // given
        Logger logger = mockTestLogger();

        String expectedActionName = UUID.randomUUID().toString();

        LoggingActionListener<AcknowledgedResponse> listener = new LoggingActionListener<>(expectedActionName);

        AcknowledgedResponse response = mock(AcknowledgedResponse.class);

        // when
        listener.onResponse(response);

        // then
        verify(logger).info("{}: success", expectedActionName);

    }

    @Test
    public void logsOnFailure() {

        // given
        Logger logger = mockTestLogger();

        String expectedActionName = UUID.randomUUID().toString();

        LoggingActionListener<AcknowledgedResponse> listener = new LoggingActionListener<>(expectedActionName);

        Exception testException = new Exception("test exception");

        // when
        listener.onFailure(testException);

        // then
        verify(logger).error("{}: failure", expectedActionName, testException);

    }

}
