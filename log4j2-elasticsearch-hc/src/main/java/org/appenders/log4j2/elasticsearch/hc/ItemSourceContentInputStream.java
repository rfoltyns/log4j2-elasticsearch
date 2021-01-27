package org.appenders.log4j2.elasticsearch.hc;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2019 Rafal Foltynski
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

import org.apache.http.nio.entity.ContentInputStream;
import org.apache.http.nio.util.SimpleInputBuffer;
import org.appenders.log4j2.elasticsearch.ItemSource;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link ItemSource} backed {@code org.apache.http.nio.entity.ContentInputStream}.
 * When it's no longer needed, {@link #close()} MUST be called to release underlying resources.
 */
public class ItemSourceContentInputStream extends ContentInputStream {

    private final AtomicBoolean closed = new AtomicBoolean();

    protected final ItemSource<SimpleInputBuffer> buffer;

    public ItemSourceContentInputStream(ItemSource<SimpleInputBuffer> buffer) {
        super(buffer.getSource());
        this.buffer = buffer;
    }

    @Override
    public void close() {
        // MUST happen only once !!!
        if (!closed.compareAndSet(false, true)) {
            buffer.release();
        }
    }

}
