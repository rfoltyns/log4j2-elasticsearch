package org.appenders.log4j2.elasticsearch.jest;

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

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.searchbox.action.AbstractDocumentTargetedAction;
import io.searchbox.action.BulkableAction;
import io.searchbox.core.DocumentResult;
import org.appenders.log4j2.elasticsearch.ItemSource;

public class BufferedIndex extends AbstractDocumentTargetedAction<DocumentResult> implements BulkableAction<DocumentResult> {

    static final String HTTP_METHOD_NAME = "POST";

    protected final ItemSource<ByteBuf> source;

    protected BufferedIndex(Builder builder) {
        super(builder);
        this.source = builder.source;
    }

    @Override
    public String getData(Gson gson) {
        throw new UnsupportedOperationException("BufferedIndex cannot return Strings. Use getSource() instead");
    }

    @Override
    public DocumentResult createNewElasticSearchResult(String responseBody, int statusCode, String reasonPhrase, Gson gson) {
        throw new UnsupportedOperationException("BufferedIndex cannot handle String result. Use buffer-based API");
    }

    public final ItemSource<ByteBuf> getSource() {
        return this.source;
    }

    @Override
    public final String getRestMethodName() {
        return HTTP_METHOD_NAME;
    }

    @Override
    public final String getBulkMethodName() {
        return null;
    }

    public void release() {
        source.release();
    }

    public static class Builder extends AbstractDocumentTargetedAction.Builder<BufferedIndex, BufferedIndex.Builder> {

        private final ItemSource<ByteBuf> source;

        public Builder(ItemSource<ByteBuf> source) {
            this.source = source;
        }

        public BufferedIndex build() {
            if (source == null) {
                throw new IllegalArgumentException("source cannot be null");
            }
            return new BufferedIndex(this);
        }
    }

}
