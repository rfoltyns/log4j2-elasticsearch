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

import org.appenders.log4j2.elasticsearch.ComponentTemplate;
import org.appenders.log4j2.elasticsearch.EmptyItemSourceFactory;
import org.appenders.log4j2.elasticsearch.ILMPolicy;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.Operation;
import org.appenders.log4j2.elasticsearch.OperationFactoryDispatcher;
import org.appenders.log4j2.elasticsearch.SetupStep;
import org.appenders.log4j2.elasticsearch.SkippingSetupStepChain;
import org.appenders.log4j2.elasticsearch.StepProcessor;
import org.appenders.log4j2.elasticsearch.ValueResolver;

import java.util.Arrays;

import static org.appenders.log4j2.elasticsearch.hc.CreateBootstrapIndex.BOOTSTRAP_TEMPLATE;

/**
 * {@inheritDoc}
 */
class HCOperationFactoryDispatcher extends OperationFactoryDispatcher {

    public HCOperationFactoryDispatcher(
            StepProcessor<SetupStep<Request, Response>> stepProcessor,
            ValueResolver valueResolver,
            EmptyItemSourceFactory itemSourceFactory) {
        super();
        // FIXME: register in HCHttp.createSetupOps(?)
        register(ComponentTemplate.TYPE_NAME, new ComponentTemplateSetupOp(stepProcessor, valueResolver, itemSourceFactory));
        register(IndexTemplate.TYPE_NAME, new IndexTemplateSetupOp(stepProcessor, valueResolver, itemSourceFactory));
        register(ILMPolicy.TYPE_NAME, new ILMPolicySetupOp(stepProcessor, valueResolver, itemSourceFactory));
    }

}
