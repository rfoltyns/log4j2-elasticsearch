package org.appenders.log4j2.elasticsearch.bulkprocessor;

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



import com.fasterxml.jackson.databind.ObjectMapper;
import org.appenders.log4j2.elasticsearch.ClientProvider;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;

import static org.appenders.log4j2.elasticsearch.bulkprocessor.BulkProcessorObjectFactoryTest.createTestObjectFactoryBuilder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@PrepareForTest(TransportClient.class)
@RunWith(PowerMockRunner.class)
public class AdminOperationsTest {

    @Test
    public void passesIndexTemplateToClient() throws IOException {

        //given
        BulkProcessorObjectFactory factory = spy(createTestObjectFactoryBuilder().build());

        IndicesAdminClient indicesAdminClient = mockedIndicesAdminClient(factory);

        IndexTemplate indexTemplate = spy(IndexTemplate.newBuilder()
                .withPath("classpath:indexTemplate.json")
                .withName("testName")
                .build());

        String expectedPayload = indexTemplate.getSource().replaceAll("\\s+","");

        // when
        factory.execute(indexTemplate);

        // then
        ArgumentCaptor<PutIndexTemplateRequest> requestArgumentCaptor = ArgumentCaptor.forClass(PutIndexTemplateRequest.class);
        verify(indicesAdminClient).putTemplate(requestArgumentCaptor.capture());

        String actualPayload = extractPayload(requestArgumentCaptor.getValue());

        Assert.assertTrue(actualPayload.contains(new ObjectMapper().readTree(expectedPayload).get("mappings").toString()));

    }

    private IndicesAdminClient mockedIndicesAdminClient(BulkProcessorObjectFactory factory) {
        ClientProvider clientProvider = mock(ClientProvider.class);
        when(factory.getClientProvider()).thenReturn(clientProvider);

        TransportClient transportClient = PowerMockito.mock(TransportClient.class);
        when(clientProvider.createClient()).thenReturn(transportClient);

        AdminClient adminClient = mock(AdminClient.class);
        when(transportClient.admin()).thenReturn(adminClient);

        IndicesAdminClient indicesAdminClient = mock(IndicesAdminClient.class);
        when(adminClient.indices()).thenReturn(indicesAdminClient);

        return indicesAdminClient;
    }

    private String extractPayload(PutIndexTemplateRequest putIndexTemplateRequest) throws IOException {
        BytesStreamOutput out = new BytesStreamOutput();
        putIndexTemplateRequest.writeTo(out);
        return new String(out.bytes().toBytesRef().bytes);
    }

}
