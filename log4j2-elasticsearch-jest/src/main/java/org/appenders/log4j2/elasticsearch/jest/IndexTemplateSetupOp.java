package org.appenders.log4j2.elasticsearch.jest;

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

import io.searchbox.client.JestResult;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.OpSource;
import org.appenders.log4j2.elasticsearch.Operation;
import org.appenders.log4j2.elasticsearch.OperationFactory;
import org.appenders.log4j2.elasticsearch.SetupStep;
import org.appenders.log4j2.elasticsearch.SkippingSetupStepChain;
import org.appenders.log4j2.elasticsearch.StepProcessor;
import org.appenders.log4j2.elasticsearch.ValueResolver;

import java.util.Collections;

/**
 * {@inheritDoc}
 */
public class IndexTemplateSetupOp implements OperationFactory {

    protected final StepProcessor<SetupStep<GenericJestRequest, JestResult>> stepProcessor;
    protected final ValueResolver valueResolver;

    public IndexTemplateSetupOp(
            StepProcessor<SetupStep<GenericJestRequest, JestResult>> stepProcessor,
            ValueResolver valueResolver) {
        this.stepProcessor = stepProcessor;
        this.valueResolver = valueResolver;
    }

    @Override
    public <T extends OpSource> Operation create(T opSource) {
        IndexTemplate indexTemplate = (IndexTemplate) opSource;

        SetupStep<GenericJestRequest, JestResult> putIndexTemplate =
                new PutIndexTemplate(
                        indexTemplate.getName(),
                        valueResolver.resolve(indexTemplate.getSource())
                );

        return new SkippingSetupStepChain(Collections.singletonList(putIndexTemplate), stepProcessor);
    }

}
