package org.appenders.log4j2.elasticsearch.ahc;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2022 Rafal Foltynski
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

import org.appenders.log4j2.elasticsearch.ahc.discovery.ServerInfo;
import org.appenders.log4j2.elasticsearch.ahc.discovery.ServerInfoListener;

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

    private static final String NAME = ServerPool.class.getSimpleName();

    private final int waitForHostsInterval = Integer.parseInt(System.getProperty("appenders.ServerPool.wait.interval", "200"));
    private final int waitForHostsRetries = Integer.parseInt(System.getProperty("appenders.ServerPool.wait.retries", "5"));

    private final AtomicInteger currentIndex = new AtomicInteger();
    private final AtomicReference<List<ServerInfo>> ref;

    public ServerPool(final List<String> addresses) {

        if (addresses == null) {
            throw new IllegalArgumentException("Initial addresses cannot be null");
        }

        final List<ServerInfo> resolved = new ArrayList<>(addresses.size());
        for (String initial : addresses) {
            final ServerInfo serverInfo = new ServerInfo(initial);
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

        final List<ServerInfo> serverInfos = ref.get();

        if (serverInfos.size() == 0) {
            throw new IllegalStateException("No servers available after " + waitForHostsRetries + " retries");
        }

        final int next = Math.abs(currentIndex.getAndIncrement() % serverInfos.size());

        final String resolvedAddress = serverInfos.get(next).getResolvedAddress();
        getLogger().debug("{}: Returning {}", NAME, resolvedAddress);

        return resolvedAddress;

    }

    @Override
    public boolean onServerInfo(final List<ServerInfo> serverInfos) {
        ref.set(serverInfos);
        return true;
    }

}
