package org.appenders.log4j2.elasticsearch.hc;

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
import org.appenders.log4j2.elasticsearch.ItemSource;

/**
 * Helper class for {@link ItemSource} writes.
 *
 * NOTE: Consider this class <i>private</i>. This class MAY be removed in future releases.
 *
 */
class ByteBufItemSourceWriter implements ItemSourceWriter<ByteBuf> {

    /**
     * Writes given bytes to given {@link ItemSource}
     *
     * @param itemSource {@link ItemSource} to operate on
     * @param bytes bytes to write
     * @return given {@link ItemSource}
     */
    @Override
    public ItemSource<ByteBuf> write(ItemSource<ByteBuf> itemSource, byte[] bytes) {
        itemSource.getSource().writeBytes(bytes);
        return itemSource;
    }

}
