package org.appenders.log4j2.elasticsearch;

import java.io.OutputStream;

public interface OutputStreamSource {

    OutputStream asOutputStream();

    OutputStream asOutputStream(OutputStream outputStream);

}
