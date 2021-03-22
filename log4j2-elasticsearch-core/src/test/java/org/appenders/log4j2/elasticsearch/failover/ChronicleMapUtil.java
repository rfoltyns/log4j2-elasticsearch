package org.appenders.log4j2.elasticsearch.failover;

import net.openhft.chronicle.map.ChronicleMap;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.smoke.SmokeTestBase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

@Disabled
public class ChronicleMapUtil {

    @Test
    public void readChronicleMap() throws IOException {

        final SmokeTestBase.SmokeTestConfig testConfig = new SmokeTestBase.SmokeTestConfig();
        testConfig.add("indexName", System.getProperty("smokeTest.indexName"));

        final ChronicleMapRetryFailoverPolicy.Builder chronicleMapFactory = new ChronicleMapRetryFailoverPolicy.Builder()
                .withKeySequenceSelector(new Log4j2SingleKeySequenceSelector.Builder()
                        .withSequenceId(1)
                        .build())
                .withFileName(resolveChronicleMapFilePath(testConfig.getProperty("indexName", String.class) + ".chronicleMap"))
                .withNumberOfEntries(1000000)
                .withAverageValueSize(2048)
                .withBatchSize(5000)
                .withRetryDelay(4000)
                .withMonitored(true)
                .withMonitorTaskInterval(1000);

        final ChronicleMap<CharSequence, ItemSource> chronicleMap = chronicleMapFactory.createChronicleMap();

        final KeySequenceSelector keySequenceSelector = chronicleMapFactory.configuredKeySequenceSelector(new ChronicleMapProxy(chronicleMap));

        final KeySequenceConfig keySequenceConfig = keySequenceSelector.currentKeySequence().get().getConfig(true);

        System.out.println(keySequenceConfig.toString());

        long minKey = Long.MAX_VALUE;
        long maxKey = Long.MIN_VALUE;
        for (CharSequence key : chronicleMap.keySet()) {

            final UUID uuid = UUID.fromString(key.toString());

            if (uuid.getLeastSignificantBits() <= minKey && uuid.getLeastSignificantBits() >= UUIDSequence.RESERVED_KEYS) {
                minKey = uuid.getLeastSignificantBits();
            }

            if (uuid.getLeastSignificantBits() >= maxKey) {
                maxKey = uuid.getLeastSignificantBits();
            }

        }

        System.out.printf("Map min: %d, max: %d; KeySequence min: %d, max: %d%n",
                minKey == Long.MAX_VALUE ? 0 : minKey,
                maxKey,
                keySequenceConfig.nextReaderIndex(),
                keySequenceConfig.nextWriterIndex());
    }

    public static String resolveChronicleMapFilePath(String fileName) {

        String path = System.getProperty(
                "appenders.failover.chroniclemap.dir",
                "./");

        if (!path.endsWith("/")) {
            path += "/";
        }

        return path + fileName;

    }

}
