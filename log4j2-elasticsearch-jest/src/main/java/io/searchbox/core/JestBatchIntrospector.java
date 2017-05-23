package io.searchbox.core;

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


import java.util.List;
import java.util.stream.Collectors;

import org.appenders.log4j2.elasticsearch.BatchIntrospector;
import org.appenders.log4j2.elasticsearch.BatchItemIntrospector;

import io.searchbox.action.AbstractDocumentTargetedAction;
import io.searchbox.action.JestActionIntrospector;

public class JestBatchIntrospector implements BatchIntrospector<Bulk> {

    private BatchItemIntrospector<AbstractDocumentTargetedAction<DocumentResult>> itemIntrospector;

    @Override
    public List<String> items(Bulk introspected) {
        return introspected.bulkableActions
            .stream()
            .map(item -> itemIntrospector().getPayload((AbstractDocumentTargetedAction<DocumentResult>) item))
            .collect(Collectors.toList());
    }

    public BatchItemIntrospector<AbstractDocumentTargetedAction<DocumentResult>> itemIntrospector() {
        if (itemIntrospector == null) {
            itemIntrospector= new JestActionIntrospector();
        }
        return itemIntrospector;
    }

}
