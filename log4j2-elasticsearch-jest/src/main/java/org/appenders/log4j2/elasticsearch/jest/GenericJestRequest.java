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

import io.searchbox.action.AbstractAction;
import io.searchbox.action.GenericResultAbstractAction;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.ElasticsearchVersion;

/**
 * Extended, lightweight Jest request
 */
public abstract class GenericJestRequest extends GenericResultAbstractAction {

    private static final AbstractAction.Builder<GenericJestRequest, JestResult> EMPTY_BUILDER = new EmptyBuilder();
    private final String httpMethodName;

    protected GenericJestRequest(String httpMethodName, String source) {
        super(EMPTY_BUILDER);
        this.httpMethodName = httpMethodName;
        this.payload = source;
    }

    public abstract String buildURI();

    @Override
    protected final String buildURI(ElasticsearchVersion elasticsearchVersion) {
        return buildURI();
    }

    @Override
    public final String getRestMethodName() {
        return httpMethodName;
    }

    static class EmptyBuilder extends AbstractAction.Builder<GenericJestRequest, JestResult> {

        @Override
        public GenericJestRequest build() {
            throw new UnsupportedOperationException("No need to use builder. Create directly");
        }

    }
}

