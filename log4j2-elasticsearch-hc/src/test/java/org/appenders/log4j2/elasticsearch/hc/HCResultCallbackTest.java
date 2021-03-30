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

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HCResultCallbackTest {

    @Test
    public void doesNotFailWhenEntityIsNull() throws IOException {

        // given
        BasicResponse basicResponse = new BasicResponse();

        ResponseHandler<Response> responseHandler = mock(ResponseHandler.class);
        when(responseHandler.deserializeResponse(isNull())).thenReturn(basicResponse);

        HCResultCallback<Response> callback = new HCResultCallback<>(responseHandler);

        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getEntity()).thenReturn(null);

        int expectedStatusCode = 1;
        String expectedReason = UUID.randomUUID().toString();
        when(httpResponse.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, expectedStatusCode, expectedReason));

        // when
        callback.completed(httpResponse);

        // then
        verify(responseHandler).deserializeResponse(isNull());
        verify(responseHandler, never()).failed(any());

    }

}
