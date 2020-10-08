package org.appenders.log4j2.elasticsearch.bulkprocessor;

import org.appenders.log4j2.elasticsearch.Result;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.support.master.TestAcknowledgedResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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