package org.appenders.log4j2.elasticsearch;

class DummySetupStep extends SetupStep<Object, Object> {

    @Override
    public Object createRequest() {
        return null;
    }

    @Override
    public Result onResponse(Object response) {
        return null;
    }

}