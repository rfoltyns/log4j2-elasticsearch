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

import org.appenders.log4j2.elasticsearch.hc.discovery.ServerInfo;
import org.appenders.log4j2.elasticsearch.hc.discovery.ServerInfoListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * Stores a list of target nodes.
 */
public class ServerPool implements ServerInfoListener {

    private final int waitForHostsInterval = Integer.parseInt(System.getProperty("appenders.ServerPool.wait.interval", "500"));
    private final int waitForHostsRetries = Integer.parseInt(System.getProperty("appenders.ServerPool.wait.retries", "5"));

    private final AtomicInteger currentIndex = new AtomicInteger();
    private final AtomicReference<List<ServerInfo>> ref;

    public ServerPool(List<String> addresses) {

        if (addresses == null) {
            throw new IllegalArgumentException("Initial addresses cannot be null");
        }

        List<ServerInfo> resolved = new ArrayList<>(addresses.size());
        for (String initial : addresses) {
            ServerInfo serverInfo = new ServerInfo(initial);
            resolved.add(serverInfo);
        }

        this.ref = new AtomicReference<>(resolved);

    }

    /**
     * This method will return next address from the list of last updated hosts
     *
     * @return next target server regardless of availability
     */
    public String getNext() {

        int retries = waitForHostsRetries;
        while (ref.get().size() == 0 && retries-- > 0) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(waitForHostsInterval));
            getLogger().warn("No servers available");
        }

        List<ServerInfo> serverInfos = ref.get();

        if (serverInfos.size() == 0) {
            throw new IllegalStateException("No servers available after " + waitForHostsRetries + " retries");
        }

        int next = Math.abs(currentIndex.getAndIncrement() % serverInfos.size());

        String resolvedAddress = serverInfos.get(next).getResolvedAddress();
        getLogger().debug("{}: Returning {}", ServerPool.class.getSimpleName(), resolvedAddress);

        return resolvedAddress;

    }

    @Override
    public boolean onServerInfo(List<ServerInfo> serverInfos) {
        ref.set(serverInfos);
        return true;
    }

}
