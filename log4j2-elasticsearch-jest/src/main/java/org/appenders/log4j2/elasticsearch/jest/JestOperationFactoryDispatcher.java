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
import org.appenders.log4j2.elasticsearch.ComponentTemplate;
import org.appenders.log4j2.elasticsearch.DataStream;
import org.appenders.log4j2.elasticsearch.ILMPolicy;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.OperationFactoryDispatcher;
import org.appenders.log4j2.elasticsearch.SetupStep;
import org.appenders.log4j2.elasticsearch.StepProcessor;
import org.appenders.log4j2.elasticsearch.ValueResolver;

/**
 * {@inheritDoc}
 */
class JestOperationFactoryDispatcher extends OperationFactoryDispatcher {

    public JestOperationFactoryDispatcher(
            StepProcessor<SetupStep<GenericJestRequest, JestResult>> stepProcessor,
            ValueResolver valueResolver) {
        register(ComponentTemplate.TYPE_NAME, new ComponentTemplateSetupOp(stepProcessor, valueResolver));
        register(IndexTemplate.TYPE_NAME, new IndexTemplateSetupOp(stepProcessor, valueResolver));
        register(ILMPolicy.TYPE_NAME, new ILMPolicySetupOp(stepProcessor, valueResolver));
        register(DataStream.TYPE_NAME, new DataStreamSetupOp(stepProcessor));
    }

}
