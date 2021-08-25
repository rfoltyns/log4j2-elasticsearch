package org.appenders.log4j2.elasticsearch;

import java.io.OutputStream;

public class ReusableOutputStreamProvider<BUF> implements OutputStreamProvider<BUF> {

    private volatile OutputStream outputStream;

    @Override
    public OutputStream asOutputStream(OutputStreamSource source) {

        if (outputStream == null) {
            outputStream = source.asOutputStream();
        }

        source.asOutputStream(outputStream);
        return outputStream;

    }

}
