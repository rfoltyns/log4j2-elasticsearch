package org.appenders.log4j2.elasticsearch.hc;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2019 Rafal Foltynski
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

import io.netty.buffer.ByteBuf;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class IndexTemplateRequestTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void restMethodNameIsPut() {

        // given
        IndexTemplateRequest request = createDefaultTestObjectBuilder().build();

        // when
        String restMethodName = request.getHttpMethodName();

        // then
        assertEquals("PUT", restMethodName);
    }

    @Test
    public void uriContainsRequestType() {

        // given
        IndexTemplateRequest request = createDefaultTestObjectBuilder().build();

        // when
        String uri = request.getURI();

        // then
        assertTrue(uri.startsWith("_template"));

    }

    @Test
    public void uriContainsTemplateName() {

        // given
        String expectedTemplateName = UUID.randomUUID().toString();

        IndexTemplateRequest request = createDefaultTestObjectBuilder()
                .withTemplateName(expectedTemplateName)
                .build();

        // when
        String uri = request.getURI();

        // then
        assertTrue(uri.endsWith(expectedTemplateName));

    }

    @Test
    public void serializeRequestWrapsSource() {

        // given
        ByteBuf buffer = mock(ByteBuf.class);
        IndexTemplateRequest request = createDefaultTestObjectBuilder()
                .withSource(buffer)
                .build();

        // when
        ItemSource<ByteBuf> itemSource = request.serialize();

        // then
        assertEquals(buffer, itemSource.getSource());

    }

    static IndexTemplateRequest.Builder createDefaultTestObjectBuilder() {
        return new IndexTemplateRequest.Builder()
                .withTemplateName("test_template")
                .withSource(mock(ByteBuf.class));
    }

}
