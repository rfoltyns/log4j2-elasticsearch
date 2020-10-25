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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class SetupOperationFactoryTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void supportsIndexTemplate() {

        // given
        final SetupOperationFactory setupOperationFactory = spy(createTestSetupOps());
        IndexTemplate opSource = IndexTemplateTest.createTestIndexTemplateBuilder().build();

        // when
        setupOperationFactory.create(opSource);

        // then
        verify(setupOperationFactory).indexTemplate(opSource);

    }

    @Test
    public void supportsILMPolicy() {

        // given
        final SetupOperationFactory setupOperationFactory = spy(createTestSetupOps());
        ILMPolicy opSource = ILMPolicyPluginTest.createTestILMPolicyPluginBuilder().build();

        // when
        setupOperationFactory.create(opSource);

        // then
        verify(setupOperationFactory).ilmPolicy(opSource);

    }

    @Test
    public void throwsOnUnknownType() {

        // given
        final SetupOperationFactory setupOperationFactory = spy(createTestSetupOps());

        OpSource opSource = new UnknownOpSource();

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage(UnknownOpSource.class.getSimpleName() + " is not supported");

        // when
        setupOperationFactory.create(opSource);

    }

    @Test
    public void canHandleUnsupportedTypesIfOverridden() {

        // given
        final SetupOperationFactory setupOperationFactory = spy(new SetupOperationFactory() {
            @Override
            public Operation indexTemplate(IndexTemplate indexTemplate) {
                return null;
            }

            @Override
            public Operation ilmPolicy(ILMPolicy ilmPolicy) {
                return null;
            }

            @Override
            public Operation handleUnsupported(OpSource opSource) {
                return new SupportedOpSource();
            }
        });

        OpSource opSource = new UnknownOpSource();

        // when
        Operation operation = setupOperationFactory.create(opSource);

        // then
        assertNotNull(operation);
        verify(setupOperationFactory).handleUnsupported(opSource);

    }

    private SetupOperationFactory createTestSetupOps() {
        return new SetupOperationFactory() {
            @Override
            public Operation indexTemplate(IndexTemplate indexTemplate) {
                return null;
            }

            @Override
            public Operation ilmPolicy(ILMPolicy ilmPolicy) {
                return null;
            }
        };
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

    private static class SupportedOpSource implements Operation {
        @Override
        public void execute() {
        }
    }
}
