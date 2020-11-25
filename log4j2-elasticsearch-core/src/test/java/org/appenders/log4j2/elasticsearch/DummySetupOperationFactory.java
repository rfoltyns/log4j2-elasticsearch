package org.appenders.log4j2.elasticsearch;

public class DummySetupOperationFactory implements OperationFactory {
    @Override
    public <T extends OpSource> Operation create(T opSource) {
        return () -> {};
    }
}
