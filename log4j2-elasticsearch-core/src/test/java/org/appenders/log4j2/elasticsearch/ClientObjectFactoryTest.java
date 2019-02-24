package org.appenders.log4j2.elasticsearch;

import org.junit.Test;

import java.util.Collection;
import java.util.function.Function;

public class ClientObjectFactoryTest {

    @Test
    public void addOperationHasDefaultImpl() {

        // given
        ClientObjectFactory factory = new ClientObjectFactory() {

            @Override
            public Collection<String> getServerList() {
                return null;
            }

            @Override
            public Object createClient() {
                return null;
            }

            @Override
            public Function createBatchListener(FailoverPolicy failoverPolicy) {
                return null;
            }

            @Override
            public Function createFailureHandler(FailoverPolicy failover) {
                return null;
            }

            @Override
            public BatchOperations createBatchOperations() {
                return null;
            }

            @Override
            public void execute(IndexTemplate indexTemplate) {

            }

        };

        // when
        factory.addOperation(() -> {});

    }

}
