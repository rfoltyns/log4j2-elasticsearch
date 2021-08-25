package org.appenders.log4j2.elasticsearch;

import java.io.OutputStream;

public interface OutputStreamProvider<BUF> {

    default OutputStream asOutputStream(ItemSource<BUF> provider) {

        if (provider instanceof OutputStreamSource) {
            return asOutputStream((OutputStreamSource) provider);
        }

        throw new IllegalArgumentException("Not an instance of " + OutputStreamSource.class.getSimpleName());

    }

    OutputStream asOutputStream(OutputStreamSource opsProvider);

}
