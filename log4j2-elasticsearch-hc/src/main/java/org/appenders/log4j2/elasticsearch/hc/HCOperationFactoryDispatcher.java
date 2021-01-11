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
import org.appenders.log4j2.elasticsearch.LifeCycle;
import org.appenders.log4j2.elasticsearch.OperationFactoryDispatcher;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.SetupStep;
import org.appenders.log4j2.elasticsearch.StepProcessor;
import org.appenders.log4j2.elasticsearch.UnlimitedResizePolicy;
import org.appenders.log4j2.elasticsearch.ValueResolver;

/**
 * {@inheritDoc}
 */
public class HCOperationFactoryDispatcher extends OperationFactoryDispatcher implements LifeCycle {

    private volatile State state = State.STOPPED;

    private final EmptyItemSourceFactory itemSourceFactory;

    public HCOperationFactoryDispatcher(
            StepProcessor<SetupStep<Request, Response>> stepProcessor,
            ValueResolver valueResolver,
            EmptyItemSourceFactory itemSourceFactory) {
        super();
        this.itemSourceFactory = itemSourceFactory;
        register(ComponentTemplate.TYPE_NAME, new ComponentTemplateSetupOp(stepProcessor, valueResolver, this.itemSourceFactory));
        register(IndexTemplate.TYPE_NAME, new IndexTemplateSetupOp(stepProcessor, valueResolver, this.itemSourceFactory));
        register(ILMPolicy.TYPE_NAME, new ILMPolicySetupOp(stepProcessor, valueResolver, this.itemSourceFactory));
    }

    public HCOperationFactoryDispatcher(
            StepProcessor<SetupStep<Request, Response>> stepProcessor,
            ValueResolver valueResolver) {
        this(stepProcessor, valueResolver, createSetupOpsItemSourceFactory());
    }

    private static PooledItemSourceFactory createSetupOpsItemSourceFactory() {
        return PooledItemSourceFactory.newBuilder()
                .withItemSizeInBytes(4096)
                .withMaxItemSizeInBytes(4096)
                .withInitialPoolSize(1)
                .withResizePolicy(UnlimitedResizePolicy.newBuilder().withResizeFactor(1).build())
                .build();
    }


    @Override
    public void start() {

        if (isStarted()) {
            return;
        }

        LifeCycle.of(itemSourceFactory).start();

        state = State.STARTED;

    }

    @Override
    public void stop() {

        if (isStopped()) {
            return;
        }

        LifeCycle.of(itemSourceFactory).stop();

        state = State.STOPPED;

    }

    @Override
    public boolean isStarted() {
        return state == State.STARTED;
    }

    @Override
    public boolean isStopped() {
        return state == State.STOPPED;
    }
}
