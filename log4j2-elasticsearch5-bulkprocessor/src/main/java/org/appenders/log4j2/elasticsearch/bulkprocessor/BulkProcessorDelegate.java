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



import org.appenders.log4j2.elasticsearch.BatchEmitter;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexRequest;

public class BulkProcessorDelegate implements BatchEmitter {

    private State state = State.STOPPED;

    private final BulkProcessor bulkProcessor;

    public BulkProcessorDelegate(BulkProcessor bulkProcessor) {
        this.bulkProcessor = bulkProcessor;
    }

    @Override
    public void add(Object batchItem) {
        bulkProcessor.add((IndexRequest) batchItem);
    }

    @Override
    public void start() {
        state = State.STARTED;
    }

    @Override
    public void stop() {
        bulkProcessor.flush();
        bulkProcessor.close();
        state = State.STOPPED;
    }

    @Override
    public boolean isStarted() {
        return state == State.STARTED;
    }

    @Override
    public boolean isStopped() {
        return state == State.STOPPED;
    }

}
