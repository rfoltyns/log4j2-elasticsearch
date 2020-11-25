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
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.ItemSource;
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

    protected final StepProcessor<SetupStep<Request, Response>> stepProcessor;
    protected final ValueResolver valueResolver;
    protected final EmptyItemSourceFactory itemSourceFactory;

    private final ByteBufItemSourceWriter writer = new ByteBufItemSourceWriter();

    public IndexTemplateSetupOp(
            StepProcessor<SetupStep<Request, Response>> stepProcessor,
            ValueResolver valueResolver,
            EmptyItemSourceFactory itemSourceFactory) {
        this.stepProcessor = stepProcessor;
        this.valueResolver = valueResolver;
        this.itemSourceFactory = itemSourceFactory;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public <T extends OpSource> Operation create(T opSource) {
        IndexTemplate indexTemplate = (IndexTemplate) opSource;

        ItemSource emptySource = itemSourceFactory.createEmptySource();

        final SetupStep<Request, Response> putIndexTemplate = new PutIndexTemplate(
                indexTemplate.getName(),
                writer.write(emptySource, valueResolver.resolve(indexTemplate.getSource()).getBytes())
        );

        return new SkippingSetupStepChain<>(Collections.singletonList(putIndexTemplate), stepProcessor);
    }

}
