package org.appenders.log4j2.elasticsearch.hc;

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

import org.appenders.log4j2.elasticsearch.EmptyItemSourceFactory;
import org.appenders.log4j2.elasticsearch.ILMPolicy;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.Operation;
import org.appenders.log4j2.elasticsearch.SetupOperationFactory;
import org.appenders.log4j2.elasticsearch.SetupStep;
import org.appenders.log4j2.elasticsearch.SkippingSetupStepChain;
import org.appenders.log4j2.elasticsearch.StepProcessor;
import org.appenders.log4j2.elasticsearch.ValueResolver;

import java.util.Arrays;
import java.util.Collections;

import static org.appenders.log4j2.elasticsearch.hc.CreateBootstrapIndex.BOOTSTRAP_TEMPLATE;

/**
 * {@inheritDoc}
 */
public class HCSetupOperationFactory extends SetupOperationFactory {

    protected final StepProcessor<SetupStep<Request, Response>> stepProcessor;
    protected final ValueResolver valueResolver;
    protected final EmptyItemSourceFactory itemSourceFactory;

    private final ByteBufItemSourceWriter writer = new ByteBufItemSourceWriter();

    public HCSetupOperationFactory(
            StepProcessor<SetupStep<Request, Response>> stepProcessor,
            ValueResolver valueResolver,
            EmptyItemSourceFactory itemSourceFactory) {
        this.stepProcessor = stepProcessor;
        this.valueResolver = valueResolver;
        this.itemSourceFactory = itemSourceFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Operation indexTemplate(IndexTemplate indexTemplate) {
        SetupStep<Request, Response> putIndexTemplate = new PutIndexTemplate(
                indexTemplate.getName(),
                toItemSource(valueResolver.resolve(indexTemplate.getSource()))
        );

        return new SkippingSetupStepChain<>(Collections.singletonList(putIndexTemplate), stepProcessor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Operation ilmPolicy(ILMPolicy ilmPolicy) {

        SetupStep<Request, Response> checkBootstrapIndex =
                new CheckBootstrapIndex(ilmPolicy.getRolloverAlias());

        String bootstrapIndexRequestBody = String.format(BOOTSTRAP_TEMPLATE, ilmPolicy.getRolloverAlias());
        SetupStep<Request, Response> createBootstrapIndex = new CreateBootstrapIndex(
                ilmPolicy.getRolloverAlias(),
                toItemSource(bootstrapIndexRequestBody));

        String ilmPolicyRequestBody = valueResolver.resolve(ilmPolicy.getSource());
        SetupStep<Request, Response> updateIlmPolicy = new PutILMPolicy(
                ilmPolicy.getName(),
                toItemSource(ilmPolicyRequestBody));

        return new SkippingSetupStepChain<>(Arrays.asList(checkBootstrapIndex, createBootstrapIndex, updateIlmPolicy), stepProcessor);
    }

    private ItemSource toItemSource(String source) {
        ItemSource emptySource = itemSourceFactory.createEmptySource();
        return writer.write(emptySource, source.getBytes());
    }

}
