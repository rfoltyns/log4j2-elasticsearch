package org.appenders.log4j2.elasticsearch;

public interface ReusableOutputStream<BUF> {

    void reset(BUF buffer);

}
