package org.appenders.log4j2.elasticsearch.hc;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2021 Rafal Foltynski
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

import com.fasterxml.jackson.databind.ObjectReader;
import org.appenders.log4j2.elasticsearch.Deserializer;
import org.appenders.log4j2.elasticsearch.JacksonDeserializer;
import org.appenders.log4j2.elasticsearch.Result;
import org.appenders.log4j2.elasticsearch.SetupStep;
import org.appenders.log4j2.elasticsearch.StepProcessor;

import java.util.function.Function;

public class SyncStepProcessor implements StepProcessor<SetupStep<Request, Response>> {

    private final HttpClientProvider clientProvider;
    private final Deserializer responseDeserializer;

    /**
     * @param clientProvider HTTP client config
     * @param objectReader deprecated reader
     * @deprecated As of 1.7, this constructor will be removed. Use {@link #SyncStepProcessor(HttpClientProvider, Deserializer)} instead
     */
    @Deprecated
    public SyncStepProcessor(final HttpClientProvider clientProvider, final ObjectReader objectReader) {
        this(clientProvider, new JacksonDeserializer<>(objectReader));
    }

    public SyncStepProcessor(final HttpClientProvider clientProvider, final Deserializer responseDeserializer) {
        this.responseDeserializer = responseDeserializer;
        this.clientProvider = clientProvider;
    }

    @Override
    public Result process(SetupStep<Request, Response> setupStep) {
        Response response = clientProvider.createClient().execute(
                setupStep.createRequest(),
                createBlockingResponseHandler()
        );

        return setupStep.onResponse(response);
    }

    /* visible for testing */
    final BlockingResponseHandler<BasicResponse> createBlockingResponseHandler() {
        return new BlockingResponseHandler<>(
                this.responseDeserializer,
                createBlockingResponseFallbackHandler()
        );
    }

    /* visible for testing */
    final Function<Exception, BasicResponse> createBlockingResponseFallbackHandler() {
        return (ex) -> {
            BasicResponse basicResponse = new BasicResponse();
            if (ex != null) {
                basicResponse.withErrorMessage(ex.getMessage());
            }
            return basicResponse;
        };
    }

}
