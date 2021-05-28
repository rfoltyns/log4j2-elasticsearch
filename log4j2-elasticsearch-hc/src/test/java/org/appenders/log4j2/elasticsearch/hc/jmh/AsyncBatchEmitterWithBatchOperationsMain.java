package org.appenders.log4j2.elasticsearch.hc.jmh;

import org.openjdk.jmh.infra.Blackhole;

public class AsyncBatchEmitterWithBatchOperationsMain {

    public static void main(String[] args) {
        AsyncBatchEmitterWithBatchOperationsTest test = new AsyncBatchEmitterWithBatchOperationsTest();
        test.itemPoolSize = 1;
        test.itemSizeInBytes = 1024;

        test.prepare();

        int limit = 1000000000;
        final Blackhole blackhole = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");
        for (int i = 0; i < limit; i++) {
            test.smokeTest(blackhole);
        }

    }
}
