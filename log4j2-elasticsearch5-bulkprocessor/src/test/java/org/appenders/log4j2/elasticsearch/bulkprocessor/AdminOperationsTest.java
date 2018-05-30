package org.appenders.log4j2.elasticsearch.bulkprocessor;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2017 Rafal Foltynski
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
