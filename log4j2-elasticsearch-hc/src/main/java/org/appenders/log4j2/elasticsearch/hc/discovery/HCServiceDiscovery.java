package org.appenders.log4j2.elasticsearch.hc.discovery;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2020 Rafal Foltynski
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

import org.appenders.log4j2.elasticsearch.ClientProvider;
import org.appenders.log4j2.elasticsearch.LifeCycle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.appenders.core.logging.InternalLogging.getLogger;

/**
 * Notifies configured listeners of changes in the address list.
 *
 * @param <T> client type
 */
public class HCServiceDiscovery<T> implements ServiceDiscovery, LifeCycle {

    private volatile State state = State.STOPPED;

    private final ClientProvider<T> clientProvider;
    private final ServiceDiscoveryRequest<T> serviceDiscoveryRequest;
    private final long refreshInterval;

    private final List<ServerInfoListener> listeners = new ArrayList<>();
    private final Map<String, ServerInfo> cache = new ConcurrentHashMap<>();

    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable);
                thread.setName("ServiceDiscovery");
                thread.setDaemon(true);
                return thread;
            });

    /**
     * @param clientProvider client to use
     * @param serviceDiscoveryRequest client-specific service discovery request
     * @param refreshInterval millis after previous request was completed
     */
    public HCServiceDiscovery(
            ClientProvider<T> clientProvider,
            ServiceDiscoveryRequest<T> serviceDiscoveryRequest,
            long refreshInterval) {
        this.refreshInterval = refreshInterval;
        this.clientProvider = clientProvider;
        this.serviceDiscoveryRequest = serviceDiscoveryRequest;
    }

    @Override
    public void addListener(ServerInfoListener listener) {
        this.cache.clear();
        this.listeners.add(listener);
    }

    public void refresh() {

        if (!isStarted()) {
            throw new IllegalStateException(HCServiceDiscovery.class.getSimpleName() + " not started");
        }

        getLogger().debug("{} : Refreshing address list", HCServiceDiscovery.class.getSimpleName());

        serviceDiscoveryRequest.execute(clientProvider.createClient(), new ServiceDiscoveryCallback());

    }

    private void processResult(final List<String> addresses) {

        if (addresses.isEmpty()) {
            return;
        }

        int previousCacheSize = cache.size();

        final List<ServerInfo> lastResult = new ArrayList<>();
        for (String address : addresses) {
            lastResult.add(cachedResult(address));
        }

        if (addresses.size() != cache.size()) {
            removeStaleEntries(lastResult);
        } else if (previousCacheSize == cache.size()) {
            // no changes
            lastResult.clear();
            return;
        }

        for (ServerInfoListener listener : listeners) {
            listener.onServerInfo(new ArrayList<>(lastResult));
        }

        lastResult.clear();

    }

    private ServerInfo cachedResult(String address) {

        if (!cache.containsKey(address)) {
            ServerInfo serverInfo = new ServerInfo(address);
            cache.put(address, serverInfo);
            getLogger().info("{}: New address found: {}", HCServiceDiscovery.class.getSimpleName(), address);
        }

        return cache.get(address);

    }

    private void removeStaleEntries(List<ServerInfo> lastResult) {
        final Collection<ServerInfo> cachedValues = cache.values();
        cachedValues.retainAll(lastResult);
    }

    class ServiceDiscoveryCallback implements org.appenders.log4j2.elasticsearch.hc.discovery.ServiceDiscoveryCallback<List<String>> {

        @Override
        public void onSuccess(List<String> result) {
            processResult(result);
        }

        @Override
        public void onFailure(Exception e) {
            getLogger().error(HCServiceDiscovery.class.getSimpleName() + ": Unable to refresh addresses: " + e.getMessage(), e);
        }

    }
    class RefreshServerList extends Thread {

        @Override
        public void run() {
            try {
                refresh();
            } catch (Exception e) {
                getLogger().error(HCServiceDiscovery.class.getSimpleName() + ": Unable to refresh addresses: " + e.getMessage(), e);
            }
        }

    }

    @Override
    public void start() {

        if (isStarted()) {
            return;
        }

        // Shared client will cause cycles. State must be set here
        state = State.STARTED;

        LifeCycle.of(clientProvider).start();

        getLogger().debug("{}: Starting executor", HCServiceDiscovery.class.getSimpleName());

        scheduleRefreshTask();

        getLogger().debug("{}: Started", HCServiceDiscovery.class.getSimpleName());

    }

    /* visible for testing */
    void scheduleRefreshTask() {
        this.executor.scheduleWithFixedDelay(
                new RefreshServerList(),
                0,
                refreshInterval,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void stop() {

        if (isStopped()) {
            return;
        }

        // Shared client will cause cycles. State must be set here
        state = State.STOPPED;

        getLogger().debug("{}: Shutting down executor", HCServiceDiscovery.class.getSimpleName());

        executor.shutdown();

        LifeCycle.of(clientProvider).stop();

        listeners.clear();

        getLogger().debug("{}: Stopped", HCServiceDiscovery.class.getSimpleName());


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
