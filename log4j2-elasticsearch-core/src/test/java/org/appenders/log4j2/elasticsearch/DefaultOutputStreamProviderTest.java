package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2021 Rafal Foltynski
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

import io.netty.buffer.ByteBuf;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultOutputStreamProviderTest {

    @Test
    public void returnsNewOutputStreamInstances() {

        // given
        final OutputStreamSource source1 = ByteBufItemSourceTest.createTestItemSource();
        final OutputStreamSource source2 = ByteBufItemSourceTest.createTestItemSource();

        final OutputStreamProvider<ByteBuf> provider = new DefaultOutputStreamProvider<>();

        // when
        final OutputStream os1 = provider.asOutputStream(source1);
        final OutputStream os2 = provider.asOutputStream(source2);

        // then
        assertNotSame(os1, os2);

    }

    @Test
    public void throwsOnIncompatibleItemSource() {

        // given
        final ItemSource<ByteBuf> source = () -> null;

        final OutputStreamProvider<ByteBuf> provider = new DefaultOutputStreamProvider<>();

        // when
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> provider.asOutputStream(source));

        // then
        assertThat(exception.getMessage(), IsEqual.equalTo("Not an instance of " + OutputStreamSource.class.getSimpleName()));

    }

}
