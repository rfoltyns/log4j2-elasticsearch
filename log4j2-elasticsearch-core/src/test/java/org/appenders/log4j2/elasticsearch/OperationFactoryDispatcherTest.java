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

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class OperationFactoryDispatcherTest {

    @Test
    public void supportsTypeOnceFactoryRegistered() {

        // given
        final OperationFactoryDispatcher operationFactoryDispatcher = spy(createTestSetupOps());
        OperationFactory operationFactory = mock(OperationFactory.class);

        OpSource opSource = new SupportedOpSource();

        assertThrows(IllegalArgumentException.class, () -> operationFactoryDispatcher.create(opSource));
        verify(operationFactoryDispatcher).handleMissing(opSource);

        // when
        operationFactoryDispatcher.register(opSource.getType(), operationFactory);
        operationFactoryDispatcher.create(opSource);

        // then
        verify(operationFactory).create(opSource);

    }

    @Test
    public void registerOverridesPreviousOperationFactory() {

        // given
        final OperationFactoryDispatcher operationFactoryDispatcher = spy(createTestSetupOps());
        OperationFactory operationFactory1 = mock(OperationFactory.class);
        OperationFactory operationFactory2 = mock(OperationFactory.class);

        OpSource opSource = new SupportedOpSource();

        // when
        boolean overriden1 = operationFactoryDispatcher.register(opSource.getType(), operationFactory1);
        boolean overriden2 = operationFactoryDispatcher.register(opSource.getType(), operationFactory2);
        operationFactoryDispatcher.create(opSource);

        // then
        assertFalse(overriden1);
        assertTrue(overriden2);

        verify(operationFactory1, never()).create(opSource);
        verify(operationFactory2).create(opSource);

    }

    @Test
    public void throwsOnUnknownType() {

        // given
        final OperationFactoryDispatcher operationFactoryDispatcher = spy(createTestSetupOps());

        OpSource opSource = new UnknownOpSource();

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> operationFactoryDispatcher.create(opSource));

        // then
        assertThat(exception.getMessage(), containsString(UnknownOpSource.class.getSimpleName() + " is not supported"));

    }

    @Test
    public void handleMissingCanReturnValidOperation() {

        // given
        final OperationFactoryDispatcher operationFactoryDispatcher = new OperationFactoryDispatcher() {
            @Override
            public Operation handleMissing(OpSource opSource) {
                return new DummySetupOperationFactory().create(opSource);
            }
        };

        OpSource opSource = new UnknownOpSource();

        // when
        Operation operation = operationFactoryDispatcher.create(opSource);

        // then
        assertNotNull(operation);

    }

    private OperationFactoryDispatcher createTestSetupOps() {
        return new OperationFactoryDispatcher();
    }

    private static class UnknownOpSource implements OpSource {
        @Override
        public String getType() {
            return "unknown";
        }

        @Override
        public String getSource() {
            return null;
        }
    }

    private static class SupportedOpSource implements OpSource {
        @Override
        public String getType() {
            return "supported";
        }
        @Override
        public String getSource() {
            return null;
        }
    }

}
