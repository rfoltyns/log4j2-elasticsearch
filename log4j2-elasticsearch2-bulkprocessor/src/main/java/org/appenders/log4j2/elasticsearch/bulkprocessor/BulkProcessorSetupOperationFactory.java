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

import org.appenders.log4j2.elasticsearch.ILMPolicy;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.Operation;
import org.appenders.log4j2.elasticsearch.SetupOperationFactory;
import org.appenders.log4j2.elasticsearch.SkippingSetupStepChain;
import org.appenders.log4j2.elasticsearch.StepProcessor;
import org.appenders.log4j2.elasticsearch.ValueResolver;
import org.elasticsearch.action.support.master.AcknowledgedResponse;

import java.util.Collections;

/**
 * {@inheritDoc}
 */
public class BulkProcessorSetupOperationFactory extends SetupOperationFactory {

    protected final ValueResolver valueResolver;
    protected final StepProcessor<BulkProcessorSetupStep<AcknowledgedResponse>> stepProcessor;

    public BulkProcessorSetupOperationFactory(
            StepProcessor<BulkProcessorSetupStep<AcknowledgedResponse>> stepProcessor,
            ValueResolver valueResolver) {
        this.valueResolver = valueResolver;
        this.stepProcessor = stepProcessor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Operation indexTemplate(IndexTemplate indexTemplate) {
        String resolved = valueResolver.resolve(indexTemplate.getSource());
        PutIndexTemplate putIndexTemplate = new PutIndexTemplate(indexTemplate.getName(), resolved);
        return new SkippingSetupStepChain<>(Collections.singletonList(putIndexTemplate), stepProcessor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Operation ilmPolicy(ILMPolicy ilmPolicy) {
        throw new IllegalArgumentException(ilmPolicy.getClass().getSimpleName() + " is not supported");
    }

}
