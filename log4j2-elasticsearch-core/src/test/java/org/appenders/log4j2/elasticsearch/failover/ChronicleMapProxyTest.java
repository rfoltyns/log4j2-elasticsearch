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
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

public class ChronicleMapProxyTest {

    @Test
    public void putDelegatesDelegates() {

        // given
        ChronicleMap<CharSequence, ItemSource> chronicleMap = createDefaultTestChronicleMap();
        CharSequence key = mock(CharSequence.class);
        ItemSource value = mock(ItemSource.class);

        ChronicleMapProxy proxy = createDefaultTestProxy(chronicleMap);

        // when
        proxy.put(key, value);

        // then
        Mockito.verify(chronicleMap).put(eq(key), eq(value));

    }

    @Test
    public void removeDelegates() {

        // given
        ChronicleMap<CharSequence, ItemSource> chronicleMap = createDefaultTestChronicleMap();
        CharSequence key = mock(CharSequence.class);

        ChronicleMapProxy proxy = createDefaultTestProxy(chronicleMap);

        // when
        proxy.remove(key);

        // then
        Mockito.verify(chronicleMap).remove(eq(key));

    }

    @Test
    public void putAllDelegates() {

        // given
        ChronicleMap<CharSequence, ItemSource> chronicleMap = createDefaultTestChronicleMap();
        Map<CharSequence, ItemSource> map = mock(Map.class);

        ChronicleMapProxy proxy = createDefaultTestProxy(chronicleMap);

        // when
        proxy.putAll(map);

        // then
        Mockito.verify(chronicleMap).putAll(eq(map));

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void containsKeyDelegates() {

        // given
        ChronicleMap<CharSequence, ItemSource> chronicleMap = createDefaultTestChronicleMap();
        CharSequence key = mock(CharSequence.class);

        ChronicleMapProxy proxy = createDefaultTestProxy(chronicleMap);

        // when
        proxy.containsKey(key);

        // then
        Mockito.verify(chronicleMap).containsKey(eq(key));

    }

    @Test
    public void getDelegates() {

        // given
        ChronicleMap<CharSequence, ItemSource> chronicleMap = createDefaultTestChronicleMap();
        CharSequence key = mock(CharSequence.class);

        ChronicleMapProxy proxy = createDefaultTestProxy(chronicleMap);

        // when
        proxy.get(key);

        // then
        Mockito.verify(chronicleMap).get(eq(key));

    }

    @Test
    public void clearDelegates() {

        // given
        ChronicleMap<CharSequence, ItemSource> chronicleMap = createDefaultTestChronicleMap();

        ChronicleMapProxy proxy = createDefaultTestProxy(chronicleMap);

        // when
        proxy.clear();

        // then
        Mockito.verify(chronicleMap).clear();

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void keySetDelegates() {

        // given
        ChronicleMap<CharSequence, ItemSource> chronicleMap = createDefaultTestChronicleMap();

        ChronicleMapProxy proxy = createDefaultTestProxy(chronicleMap);

        // when
        proxy.keySet();

        // then
        Mockito.verify(chronicleMap).keySet();

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void valuesDelegates() {

        // given
        ChronicleMap<CharSequence, ItemSource> chronicleMap = createDefaultTestChronicleMap();

        ChronicleMapProxy proxy = createDefaultTestProxy(chronicleMap);

        // when
        proxy.values();

        // then
        Mockito.verify(chronicleMap).values();

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void entrySetDelegates() {

        // given
        ChronicleMap<CharSequence, ItemSource> chronicleMap = createDefaultTestChronicleMap();

        ChronicleMapProxy proxy = createDefaultTestProxy(chronicleMap);

        // when
        proxy.entrySet();

        // then
        Mockito.verify(chronicleMap).entrySet();

    }

    @Test
    public void sizeDelegates() {

        // given
        ChronicleMap<CharSequence, ItemSource> chronicleMap = createDefaultTestChronicleMap();

        ChronicleMapProxy proxy = createDefaultTestProxy(chronicleMap);

        // when
        proxy.size();

        // then
        Mockito.verify(chronicleMap).size();

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void isEmptyDelegates() {

        // given
        ChronicleMap<CharSequence, ItemSource> chronicleMap = createDefaultTestChronicleMap();

        ChronicleMapProxy proxy = createDefaultTestProxy(chronicleMap);

        // when
        proxy.isEmpty();

        // then
        Mockito.verify(chronicleMap).isEmpty();

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void containsValueDelegates() {

        // given
        ChronicleMap<CharSequence, ItemSource> chronicleMap = createDefaultTestChronicleMap();
        ItemSource value = mock(ItemSource.class);

        ChronicleMapProxy proxy = createDefaultTestProxy(chronicleMap);

        // when
        proxy.containsValue(value);

        // then
        Mockito.verify(chronicleMap).containsValue(eq(value));

    }

    @Test
    public void closeDelegates() {

        // given
        ChronicleMap<CharSequence, ItemSource> chronicleMap = createDefaultTestChronicleMap();

        ChronicleMapProxy proxy = createDefaultTestProxy(chronicleMap);

        // when
        proxy.close();

        // then
        Mockito.verify(chronicleMap).close();

    }

    private ChronicleMapProxy createDefaultTestProxy(ChronicleMap<CharSequence, ItemSource> chronicleMap) {
        return new ChronicleMapProxy(chronicleMap);
    }

    private ChronicleMap<CharSequence, ItemSource> createDefaultTestChronicleMap() {
        return mock(ChronicleMap.class);
    }

}