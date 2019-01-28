package org.appenders.log4j2.elasticsearch;

public interface LifeCycle {

    void start();

    void stop();

    boolean isStarted();

    boolean isStopped();

    enum State {
        STARTED, STOPPED
    }

}
