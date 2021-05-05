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

import org.appenders.log4j2.elasticsearch.Result;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.support.master.TestAcknowledgedResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BulkProcessorSetupStepTest {

    @Test
    public void onResponseReturnsSuccessOnAcknowledged() {

        // given
        AcknowledgedResponse response = new TestAcknowledgedResponse(true);

        BulkProcessorSetupStep<AcknowledgedResponse> request = new DummyBulkProcessorSetupStep();

        // when
        Result result = request.onResponse(response);

        // then
        assertEquals(Result.SUCCESS, result);

    }

    @Test
    public void onResponseReturnsFailureOnNotAcknowledged() {

        // given
        AcknowledgedResponse response = new TestAcknowledgedResponse(false);

        BulkProcessorSetupStep<AcknowledgedResponse> request = new DummyBulkProcessorSetupStep();

        // when
        Result result = request.onResponse(response);

        // then
        assertEquals(Result.FAILURE, result);

    }

    private static class DummyBulkProcessorSetupStep extends BulkProcessorSetupStep<AcknowledgedResponse> {

        @Override
        public Result execute(TransportClient client) {
            return null;
        }

        @Override
        public ActionRequest createRequest() {
            return null;
        }

    }

}
