package org.appenders.log4j2.elasticsearch.jmh;

import org.openjdk.jmh.infra.Blackhole;

public class PooledItemSourceFactoryTestMain extends PooledItemSourceFactoryTest {

    public static void main(String[] args) {

        PooledItemSourceFactoryTest test = new PooledItemSourceFactoryTest();
        test.itemSizeInBytes = 8192;
        test.poolSize = 10000;

        test.prepare();

        int limit = 1000000000;
        final Blackhole blackhole = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");
        for (int i = 0; i < limit; i++) {
            test.smokeTest(blackhole);
        }
    }

}
