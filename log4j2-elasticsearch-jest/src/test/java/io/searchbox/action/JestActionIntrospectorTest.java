package io.searchbox.action;

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
import org.junit.Assert;
import org.junit.Test;

import io.searchbox.core.DocumentResult;
import io.searchbox.core.Index;

public class JestActionIntrospectorTest {

    @Test
    public void intropectorCanAccessActionPayload() {

        // given
        BatchItemIntrospector<AbstractDocumentTargetedAction<DocumentResult>> introspector = new JestActionIntrospector();
        String testPayload = "testPayload";
        AbstractDocumentTargetedAction<DocumentResult> action = new Index.Builder(testPayload).build();

        // when
        String payload = introspector.getPayload(action);

        // then
        Assert.assertEquals(testPayload, payload);
    }

}
