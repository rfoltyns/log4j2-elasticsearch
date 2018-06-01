package org.appenders.log4j2.elasticsearch.jest;

/*-
 * #%L
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
import io.searchbox.action.TemplateActionIntrospector;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.indices.template.TemplateAction;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.appenders.log4j2.elasticsearch.ClientProvider;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import java.io.IOException;

import static org.appenders.log4j2.elasticsearch.jest.JestHttpObjectFactoryTest.createTestObjectFactoryBuilder;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AdminOperationsTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void passesIndexTemplateToClient() throws IOException {

        //given
        JestHttpObjectFactory factory = spy(createTestObjectFactoryBuilder().build());

        JestClient jestClient = mockedJestClient(factory);

        mockedJestResult(jestClient, true);

        IndexTemplate indexTemplate = spy(IndexTemplate.newBuilder()
                .withPath("classpath:indexTemplate.json")
                .withName("testName")
                .build());

        String expectedPayload = indexTemplate.getSource();

        // when
        factory.execute(indexTemplate);

        // then
        ArgumentCaptor<TemplateAction> requestArgumentCaptor = ArgumentCaptor.forClass(TemplateAction.class);
        verify(jestClient).execute(requestArgumentCaptor.capture());

        String actualPayload = extractPayload(requestArgumentCaptor.getValue());

        Assert.assertEquals(actualPayload, expectedPayload);

    }

    @Test
    public void throwsIfTemplateActionNotSucceeded() throws IOException {

        //given
        JestHttpObjectFactory factory = spy(createTestObjectFactoryBuilder().build());

        JestClient jestClient = mockedJestClient(factory);

        mockedJestResult(jestClient, false);

        IndexTemplate indexTemplate = spy(IndexTemplate.newBuilder()
                .withPath("classpath:indexTemplate.json")
                .withName("testName")
                .build());

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("IndexTemplate not added");

        // when
        factory.execute(indexTemplate);

    }


    @Test
    public void throwsOnIOException() {

        //given
        JestHttpObjectFactory factory = spy(createTestObjectFactoryBuilder().build());

        final String expectedMessage = "test-exception";

        when(factory.createClient()).thenAnswer((Answer<JestResult>) invocation -> {
            throw new IOException(expectedMessage);
        });

        IndexTemplate indexTemplate = spy(IndexTemplate.newBuilder()
                .withPath("classpath:indexTemplate.json")
                .withName("testName")
                .build());

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage(expectedMessage);

        // when
        factory.execute(indexTemplate);

    }

    private void mockedJestResult(JestClient jestClient, boolean isSucceeded) throws IOException {
        JestResult result = mock(JestResult.class);
        when(jestClient.execute(any())).thenReturn(result);

        when(result.isSucceeded()).thenReturn(isSucceeded);
    }

    private JestClient mockedJestClient(JestHttpObjectFactory factory) {
        ClientProvider clientProvider = mock(ClientProvider.class);
        when(factory.getClientProvider(any())).thenReturn(clientProvider);

        JestClient jestClient = mock(JestClient.class);
        when(clientProvider.createClient()).thenReturn(jestClient);
        return jestClient;
    }

    private String extractPayload(TemplateAction templateAction) {
        return new TemplateActionIntrospector().getPayload(templateAction);
    }

}
