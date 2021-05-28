package org.appenders.log4j2.elasticsearch;

import io.netty.buffer.ByteBuf;

public class ExtendedPooledItemSourceFactory extends PooledItemSourceFactory {

    protected ExtendedPooledItemSourceFactory(ItemSourcePool bufferedItemSourcePool) {
        super(bufferedItemSourcePool);
    }

    public static class Builder extends PooledItemSourceFactory.Builder {

        @Override
        public ItemSourcePool<ByteBuf> configuredItemSourcePool() {
            return super.configuredItemSourcePool();
        }

    }
}
