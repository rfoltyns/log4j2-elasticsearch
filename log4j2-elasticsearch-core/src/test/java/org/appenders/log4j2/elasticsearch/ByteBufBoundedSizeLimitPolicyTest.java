package org.appenders.log4j2.elasticsearch;

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

import io.netty.buffer.ByteBuf;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ByteBufBoundedSizeLimitPolicyTest {

    public static final int DEFAULT_TEST_MAX_SIZE = 1024;
    public static final int DEFAULT_TEST_MIN_SIZE = DEFAULT_TEST_MAX_SIZE / 2;

    public static ByteBufBoundedSizeLimitPolicy createDefaultTestBoundedSizeLimitPolicy() {
        return new ByteBufBoundedSizeLimitPolicy(DEFAULT_TEST_MIN_SIZE, DEFAULT_TEST_MAX_SIZE);
    }

    @Test
    public void shrinksBufferIfOversized() {

        // given
        SizeLimitPolicy<ByteBuf> sizeLimitPolicy = createDefaultTestBoundedSizeLimitPolicy();

        ByteBuf byteBuf = ByteBufItemSourceTest.createDefaultTestByteBuf();

        int postWriteCapacity = DEFAULT_TEST_MAX_SIZE * 2;
        byte[] bytes = new byte[postWriteCapacity];
        Arrays.fill(bytes, (byte) 1);

        byteBuf.writeBytes(bytes);

        // when
        sizeLimitPolicy.limit(byteBuf);

        // then
        assertEquals(DEFAULT_TEST_MAX_SIZE, byteBuf.capacity());

    }

    @Test
    public void growsBufferIfUndersized() {

        // given
        int expectedCapacity = DEFAULT_TEST_MAX_SIZE / 2;
        SizeLimitPolicy<ByteBuf> sizeLimitPolicy = new ByteBufBoundedSizeLimitPolicy(expectedCapacity, DEFAULT_TEST_MAX_SIZE);

        ByteBuf byteBuf = ByteBufItemSourceTest.createDefaultTestByteBuf();

        // sanity check
        assertTrue(expectedCapacity > byteBuf.capacity());

        // when
        sizeLimitPolicy.limit(byteBuf);

        // then
        assertEquals(expectedCapacity, byteBuf.capacity());

    }

    @Test
    public void doesNotChangeCapacityIfBetweenBounds() {

        // given
        SizeLimitPolicy<ByteBuf> sizeLimitPolicy = createDefaultTestBoundedSizeLimitPolicy();

        ByteBuf byteBuf = ByteBufItemSourceTest.createDefaultTestByteBuf();
        byteBuf.capacity(DEFAULT_TEST_MIN_SIZE + 1);

        // sanity check
        assertTrue(DEFAULT_TEST_MIN_SIZE < byteBuf.capacity());
        assertTrue(DEFAULT_TEST_MAX_SIZE > byteBuf.capacity());

        int expectedCapacity = byteBuf.capacity();

        // when
        sizeLimitPolicy.limit(byteBuf);

        // then
        assertEquals(expectedCapacity, byteBuf.capacity());

    }

}
