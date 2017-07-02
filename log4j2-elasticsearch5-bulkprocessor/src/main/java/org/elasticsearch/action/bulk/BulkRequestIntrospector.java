package org.elasticsearch.action.bulk;

/*-
 * #%L
 * log4j-elasticsearch
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


import org.appenders.log4j2.elasticsearch.BatchIntrospector;
import org.appenders.log4j2.elasticsearch.BatchItemIntrospector;
import org.elasticsearch.action.index.BulkActionIntrospector;
import org.elasticsearch.action.index.IndexRequest;

import java.util.Collection;
import java.util.stream.Collectors;

public class BulkRequestIntrospector implements BatchIntrospector<BulkRequest> {

    private BulkActionIntrospector itemIntrospector;

    @Override
    public Collection<String> items(BulkRequest introspected) {
        return introspected.requests
                .stream()
                .map(item -> itemIntrospector().getPayload((IndexRequest) item))
                .collect(Collectors.toList());
    }

    public BatchItemIntrospector<IndexRequest> itemIntrospector() {
        if (itemIntrospector == null) {
            itemIntrospector= new BulkActionIntrospector();
        }
        return itemIntrospector;
    }
}
