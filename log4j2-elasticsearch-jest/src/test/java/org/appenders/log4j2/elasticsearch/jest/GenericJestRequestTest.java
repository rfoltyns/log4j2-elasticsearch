package org.appenders.log4j2.elasticsearch.jest;

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

import io.searchbox.action.GenericJestRequestIntrospector;
import io.searchbox.client.config.ElasticsearchVersion;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class GenericJestRequestTest {

    @Test
    public void builderThrowsByDefault() {

        // given
        GenericJestRequest.EmptyBuilder builder = new GenericJestRequest.EmptyBuilder();

        // when
        final UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, builder::build);

        // then
        assertThat(exception.getMessage(), containsString("No need to use builder. Create directly"));

    }

    @Test
    public void inheritedBuildURIDelegates() {

        // given
        String httpMethodName = UUID.randomUUID().toString();
        String source = UUID.randomUUID().toString();

        GenericJestRequest request = spy(new GenericJestRequest(httpMethodName, source) {
            @Override
            public String buildURI() {
                return null;
            }
        });

        // when
        request.buildURI(ElasticsearchVersion.UNKNOWN);

        // then
        verify(request).buildURI();

    }

    @Test
    public void parametersAreNotModified() {

        // given
        String httpMethodName = UUID.randomUUID().toString();
        String source = UUID.randomUUID().toString();


        // when
        GenericJestRequest request = spy(new GenericJestRequest(httpMethodName, source) {
            @Override
            public String buildURI() {
                return null;
            }
        });

        // then
        assertSame(httpMethodName, request.getRestMethodName());
        assertSame(source, GenericJestRequestIntrospector.getPayload(request));

    }

}
