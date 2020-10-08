package org.appenders.log4j2.elasticsearch.hc;

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
