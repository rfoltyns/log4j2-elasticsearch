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

import org.appenders.log4j2.elasticsearch.Deserializer;
import org.appenders.log4j2.elasticsearch.Result;
import org.appenders.log4j2.elasticsearch.SetupStep;
import org.appenders.log4j2.elasticsearch.StepProcessor;

import java.util.function.Function;

public class SyncStepProcessor implements StepProcessor<SetupStep<Request, Response>> {

    private final HttpClientProvider clientProvider;
    private final Deserializer<BatchResult> deserializer;

    public SyncStepProcessor(final HttpClientProvider clientProvider, final Deserializer<BatchResult> deserializer) {
        this.deserializer = deserializer;
        this.clientProvider = clientProvider;
    }

    @Override
    public Result process(final SetupStep<Request, Response> setupStep) {
        final Response response = clientProvider.createClient().execute(
                setupStep.createRequest(),
                createBlockingResponseHandler()
        );

        return setupStep.onResponse(response);
    }

    /* visible for testing */
    final BlockingResponseHandler<BatchResult> createBlockingResponseHandler() {
        return new BlockingResponseHandler<>(
                this.deserializer,
                createBlockingResponseFallbackHandler()
        );
    }

    /* visible for testing */
    final Function<Exception, BatchResult> createBlockingResponseFallbackHandler() {
        return (ex) -> {
            final BatchResult basicResponse = new BatchResult(0, false, null, 500, null);
            if (ex != null) {
                basicResponse.withErrorMessage(ex.getMessage());
            }
            return basicResponse;
        };
    }

}
