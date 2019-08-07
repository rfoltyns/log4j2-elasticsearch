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



import io.searchbox.action.BulkableAction;
import io.searchbox.core.Bulk;
import io.searchbox.core.Index;
import org.appenders.log4j2.elasticsearch.BatchBuilder;
import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.appenders.log4j2.elasticsearch.ItemSource;

public class JestBulkOperations implements BatchOperations<Bulk> {

    public static final String DEFAULT_MAPPING_TYPE = "index";

    /**
     * By default, "index" until 1.4, then "_doc"
     */
    private final String mappingType;

    public JestBulkOperations() {
        this.mappingType = DEFAULT_MAPPING_TYPE;
    }

    public JestBulkOperations(String mappingType) {
        this.mappingType = mappingType;
    }

    @Override
    public Object createBatchItem(String indexName, Object source) {
        return new Index.Builder(source)
                .index(indexName)
                .type(mappingType)
                .build();
    }

    @Override
    public Object createBatchItem(String indexName, ItemSource source) {
        return new Index.Builder(source.getSource())
                .index(indexName)
                .type(mappingType)
                .build();
    }

    @Override
    public BatchBuilder<Bulk> createBatchBuilder() {
        return new BatchBuilder<Bulk>() {

            private final Bulk.Builder builder = new ExtendedBulk.Builder();

            @Override
            public void add(Object item) {
                builder.addAction((BulkableAction) item);
            }

            @Override
            public Bulk build() {
                return builder.build();
            }
        };
    }

}
