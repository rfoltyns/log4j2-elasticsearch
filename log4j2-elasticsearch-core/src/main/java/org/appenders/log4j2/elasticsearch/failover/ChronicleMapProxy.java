package org.appenders.log4j2.elasticsearch.failover;

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

import net.openhft.chronicle.map.ChronicleMap;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.StampedLock;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Ensures thread-safe writes to underlying {@code ChronicleMap}
 *
 * At the moment, following calls are executed exclusively:
 * <ul>
 * <li>{@link #put(CharSequence, ItemSource)}</li>
 * <li>{@link #remove(Object)}</li>
 * </ul>
 *
 * All other methods are executed by simple delegation.
 *
 * NOTE: Consider this class <i>private</i>.
 */
class ChronicleMapProxy implements MapProxy<CharSequence, ItemSource> {

    private final ChronicleMap<CharSequence, ItemSource> chronicleMap;

    private final StampedLock stampedLock = new StampedLock();

    public ChronicleMapProxy(ChronicleMap<CharSequence, ItemSource> chronicleMap) {
        this.chronicleMap = chronicleMap;
    }

    @Override
    public ItemSource put(CharSequence key, ItemSource failedItem) {
        return executeExclusive(this::putInternal, key, failedItem); 
    }

    @Override
    public ItemSource remove(Object key) {
        return executeExclusive(this::removeInternal, (CharSequence)key);
    }

    @Override
    public void putAll(@NotNull Map<? extends CharSequence, ? extends ItemSource> map) {
        chronicleMap.putAll(map);
    }

    @Override
    public boolean containsKey(Object key) {
        return chronicleMap.containsKey(key);
    }

    @Override
    public ItemSource get(Object key) {
        return chronicleMap.get(key);
    }

    @Override
    public void clear() {
        chronicleMap.clear();
    }

    @NotNull
    @Override
    public Set<CharSequence> keySet() {
        return chronicleMap.keySet();
    }

    @NotNull
    @Override
    public Collection<ItemSource> values() {
        return chronicleMap.values();
    }

    @NotNull
    @Override
    public Set<Entry<CharSequence, ItemSource>> entrySet() {
        return chronicleMap.entrySet();
    }

    @Override
    public int size() {
        return chronicleMap.size();
    }

    @Override
    public boolean isEmpty() {
        return chronicleMap.isEmpty();
    }

    @Override
    public boolean containsValue(Object value) {
        return chronicleMap.containsValue(value);
    }

    @Override
    public void close() {
        chronicleMap.close();
    }

    private <T,R> R executeExclusive(Function<T, R> op, T arg) {

        long stamp = stampedLock.writeLock();

        try {
            return op.apply(arg);
        } finally {
            stampedLock.unlockWrite(stamp);
        }

    }

    private <T, U, R> R executeExclusive(BiFunction<T, U, R> op, T arg1, U arg2) {

        long stamp = stampedLock.writeLock();

        try {
            return op.apply(arg1, arg2);
        } finally {
            stampedLock.unlockWrite(stamp);
        }

    }

    private ItemSource putInternal(CharSequence key, ItemSource failedItem) {
        return chronicleMap.put(key, failedItem);
    }

    private ItemSource removeInternal(CharSequence key) {
        return chronicleMap.remove(key);
    }

}
