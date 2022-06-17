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

import org.appenders.log4j2.elasticsearch.DataStream;
import org.appenders.log4j2.elasticsearch.EmptyItemSourceFactory;
import org.appenders.log4j2.elasticsearch.OpSource;
import org.appenders.log4j2.elasticsearch.Operation;
import org.appenders.log4j2.elasticsearch.OperationFactory;
import org.appenders.log4j2.elasticsearch.SetupStep;
import org.appenders.log4j2.elasticsearch.SkippingSetupStepChain;
import org.appenders.log4j2.elasticsearch.StepProcessor;

import java.util.Arrays;

public class DataStreamSetupOp implements OperationFactory {

    protected final StepProcessor<SetupStep<Request, Response>> stepProcessor;
    protected final EmptyItemSourceFactory itemSourceFactory;

    public DataStreamSetupOp(final StepProcessor<SetupStep<Request, Response>> stepProcessor, final EmptyItemSourceFactory itemSourceFactory) {
        this.stepProcessor = stepProcessor;
        this.itemSourceFactory = itemSourceFactory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends OpSource> Operation create(final T opSource) {

        final DataStream dataStream = (DataStream) opSource;

        final SetupStep<Request, Response> checkDataStream =
                new CheckDataStream(dataStream.getName());

        final SetupStep<Request, Response> createDataStream = new CreateDataStream(
                dataStream.getName(),
                itemSourceFactory.createEmptySource());

        return new SkippingSetupStepChain<>(Arrays.asList(checkDataStream, createDataStream), stepProcessor);

    }

}
