package org.appenders.log4j2.elasticsearch.hc.smoke;

import org.appenders.log4j2.elasticsearch.smoke.TestConfig;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Network;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.appenders.core.logging.InternalLogging.getLogger;

public class ContainerRunner {

    private static final DockerImageName ELASTICSEARCH_IMAGE = DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch");
    public static final String CERT_BUNDLE_NAME = "certificate-bundle";
    public static final String CONTAINER_PATH = "";
    public static final String CERT_DIR_NAME = "certificate-bundle";

    private final List<ElasticsearchContainer> containers = new ArrayList<>();

    private final AtomicBoolean initialized = new AtomicBoolean();
    private final List<String> containerAddresses = new ArrayList<>();
    private final TestConfig config;

    public ContainerRunner(TestConfig config) {
        this.config = config;
    }

    public List<String> start() {
        return startContainers();
    }

    protected List<String> startContainers() {

        final List<String> newContainers = new ArrayList<>();

        if (initialized.compareAndSet(false, true)) {

            int size = 1;

            while (size-- > 0) {
                containers.add(startContainer());
            }

            for (final ElasticsearchContainer container : containers) {
                final String httpHostAddress = container.getHttpHostAddress();
                newContainers.add(httpHostAddress);
            }

            containerAddresses.addAll(newContainers);

        }

        if (containerAddresses.isEmpty()) {
            throw new IllegalStateException("Unable to start containers on time");
        }

        return containerAddresses;

    }

    private ElasticsearchContainer startContainer() {
        final String apiVersion = config.getProperty("api.version", String.class);
        final String secure = Boolean.toString(config.getProperty("secure", Boolean.class));
        final String containerCertPath = "/usr/share/elasticsearch/config/" + CERT_DIR_NAME;
        final ElasticsearchContainer container = new ElasticsearchContainer(ELASTICSEARCH_IMAGE.withTag(apiVersion))
                .withClasspathResourceMapping(CERT_BUNDLE_NAME, containerCertPath, BindMode.READ_ONLY)
                .withReuse(false)
                .withLogConsumer(outputFrame -> getLogger().warn(outputFrame.getUtf8String()))
                .withExposedPorts(9200)
                .withNetwork(Network.SHARED)
                .withEnv("CERTS_DIR", containerCertPath)
                .withPassword("doesntMatter")
                .withEnv("xpack.security.enabled", secure)
                .withEnv("xpack.security.http.ssl.enabled", secure)
                .withEnv("xpack.security.http.ssl.key", containerCertPath("pemCertInfo.keyPath"))
                .withEnv("xpack.security.http.ssl.certificate_authorities", containerCertPath("pemCertInfo.caPath"))
                .withEnv("xpack.security.http.ssl.certificate", containerCertPath("pemCertInfo.clientCertPath"))
                .withEnv("xpack.security.transport.ssl.enabled", secure)
                .withEnv("xpack.security.transport.ssl.verification_mode", "none")
                .withEnv("xpack.security.transport.ssl.certificate_authorities", containerCertPath("pemCertInfo.caPath"))
                .withEnv("xpack.security.transport.ssl.certificate", containerCertPath("pemCertInfo.clientCertPath"))
                .withEnv("xpack.security.transport.ssl.key", containerCertPath("pemCertInfo.keyPath"));

        container.start();

        return container;
    }

    private String containerCertPath(final String propertyName) {
        return ContainerRunner.CONTAINER_PATH + trimBefore(CERT_DIR_NAME, config.getProperty(propertyName, String.class));
    }

    private String trimBefore(final String match, final String value) {
        return value.substring(value.indexOf(match));
    }


    public void stop() {

        for (ElasticsearchContainer container : containers) {
            container.stop();
        }

    }
}
