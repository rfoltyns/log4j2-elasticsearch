package action;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2018 Rafal Foltynski
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


import org.appenders.log4j2.elasticsearch.BatchItemIntrospector;
import org.elasticsearch.action.index.BulkActionIntrospector;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BulkActionIntrospectorTest {

    @Test
    public void intropectorCanAccessActionPayload() {

        // given
        BatchItemIntrospector<IndexRequest> introspector = new BulkActionIntrospector();
        String testPayload = "testPayload";
        IndexRequest action = new IndexRequest().source(testPayload, XContentType.CBOR);

        // when
        String payload = (String) introspector.getPayload(action);

        // then
        assertEquals(testPayload, payload);

    }

}
