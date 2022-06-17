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

import org.appenders.log4j2.elasticsearch.EmptyItemSourceFactory;
import org.appenders.log4j2.elasticsearch.ILMPolicy;
import org.appenders.log4j2.elasticsearch.OpSource;
import org.appenders.log4j2.elasticsearch.Operation;
import org.appenders.log4j2.elasticsearch.OperationFactory;
import org.appenders.log4j2.elasticsearch.SetupStep;
import org.appenders.log4j2.elasticsearch.SkippingSetupStepChain;
import org.appenders.log4j2.elasticsearch.StepProcessor;
import org.appenders.log4j2.elasticsearch.ValueResolver;

import java.util.ArrayList;
import java.util.List;

import static org.appenders.log4j2.elasticsearch.ahc.CreateBootstrapIndex.BOOTSTRAP_TEMPLATE;

public class ILMPolicySetupOp implements OperationFactory {

    protected final StepProcessor<SetupStep<Request, Response>> stepProcessor;
    protected final ValueResolver valueResolver;
    protected final EmptyItemSourceFactory itemSourceFactory;

    private final ByteBufItemSourceWriter writer = new ByteBufItemSourceWriter();

    public ILMPolicySetupOp(final StepProcessor<SetupStep<Request, Response>> stepProcessor, final ValueResolver valueResolver, final EmptyItemSourceFactory itemSourceFactory) {
        this.stepProcessor = stepProcessor;
        this.valueResolver = valueResolver;
        this.itemSourceFactory = itemSourceFactory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends OpSource> Operation create(final T opSource) {

        final ILMPolicy ilmPolicy = (ILMPolicy) opSource;
        final List<SetupStep<Request, Response>> setupSteps = new ArrayList<>();

        if (ilmPolicy.isCreateBootstrapIndex()) {

            final SetupStep<Request, Response> checkBootstrapIndex =
                    new CheckBootstrapIndex(ilmPolicy.getRolloverAlias());

            final String bootstrapIndexRequestBody = String.format(BOOTSTRAP_TEMPLATE, ilmPolicy.getRolloverAlias());
            final SetupStep<Request, Response> createBootstrapIndex = new CreateBootstrapIndex(
                    ilmPolicy.getRolloverAlias(),
                    writer.write(itemSourceFactory.createEmptySource(), bootstrapIndexRequestBody.getBytes()));

            setupSteps.add(checkBootstrapIndex);
            setupSteps.add(createBootstrapIndex);

        }

        final String ilmPolicyRequestBody = valueResolver.resolve(ilmPolicy.getSource());
        final SetupStep<Request, Response> updateIlmPolicy = new PutILMPolicy(
                ilmPolicy.getName(),
                writer.write(itemSourceFactory.createEmptySource(), ilmPolicyRequestBody.getBytes()));

        setupSteps.add(updateIlmPolicy);

        return new SkippingSetupStepChain<>(setupSteps, stepProcessor);

    }

}
