package org.appenders.log4j2.elasticsearch;

import java.io.OutputStream;

public class DefaultOutputStreamProvider<BUF> implements OutputStreamProvider<BUF> {

    @Override
    public OutputStream asOutputStream(OutputStreamSource source) {
        return source.asOutputStream();
    }

}
