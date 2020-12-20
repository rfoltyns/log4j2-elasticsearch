package org.appenders.log4j2.elasticsearch.hc.discovery;

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

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class NodesRequestTest {

    @Test
    public void urlHasHttpParam() {

        // when
        NodesRequest request = new NodesRequest();

        // then
        assertSame(NodesRequest.PATH, request.getURI());
        assertThat(request.getURI(), CoreMatchers.endsWith("http"));

    }

    @Test
    public void isGetRequest() {

        // given
        String expectedHttpMethodName = "GET";

        // when
        NodesRequest request = new NodesRequest();

        // then
        assertSame(NodesRequest.HTTP_METHOD, request.getHttpMethodName());
        assertEquals(expectedHttpMethodName, request.getHttpMethodName());

    }

    @Test
    public void bodyIsEmpty() {

        // given
        String expectedBody = "{}";

        // when
        NodesRequest request = new NodesRequest();

        // then
        assertSame(NodesRequest.EMPTY_REQUEST_BODY.getSource(), request.serialize().getSource());
        assertEquals(expectedBody, request.serialize().getSource());

    }

}
