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


import org.appenders.log4j2.elasticsearch.BatchBuilder;
import org.appenders.log4j2.elasticsearch.BatchOperations;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.xcontent.XContentType;

public class ElasticsearchBatchOperations implements BatchOperations<BulkRequest> {

    private static final String ACTION_TYPE = "index";

    @Override
    public Object createBatchItem(String indexName, Object source) {
        return new IndexRequest(indexName)
                .type(ACTION_TYPE)
                .source((String)source, XContentType.JSON);
    }

    @Override
    public Object createBatchItem(String indexName, ItemSource source) {
        return new IndexRequest(indexName)
                .type(ACTION_TYPE)
                .source((String)source.getSource(), XContentType.JSON);
    }

    @Override
    public BatchBuilder<BulkRequest> createBatchBuilder() {
        throw new UnsupportedOperationException("No need to create BatchBuilder for ElasticsearchBulkProcessor");
    }

}
