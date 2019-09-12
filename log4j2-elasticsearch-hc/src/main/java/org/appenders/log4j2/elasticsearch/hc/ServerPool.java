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

import org.apache.logging.log4j.core.config.ConfigurationException;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Stores a list of target nodes.
 */
public class ServerPool {

    private final AtomicReference<List<String>> serverListRef;
    private final AtomicInteger currentIndex;

    public ServerPool(List<String> serverList) {
        if (serverList == null || serverList.isEmpty()) {
            throw new ConfigurationException("Initial server list must not be empty or null");
        }
        this.serverListRef = new AtomicReference<>(serverList);
        this.currentIndex = new AtomicInteger();
    }

    /**
     * @return next target server
     */
    public String getNext() {
        List<String> current = serverListRef.get();
        return current.get(currentIndex.getAndIncrement() % current.size());
    }

}
