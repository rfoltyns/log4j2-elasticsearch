package org.appenders.log4j2.elasticsearch.hc.smoke;

import org.appenders.log4j2.elasticsearch.smoke.TestConfig;
import org.appenders.log4j2.elasticsearch.util.Version;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.MountableFile;

public class Elasticsearch5Configurer implements ElasticsearchContainerConfigurer {

    @Override
    public void configure(final ElasticsearchContainer container, final Version version, final TestConfig config) {

        if (config.getProperty("secure", Boolean.class)) {

            final String keyPath = config.getProperty("pemCertInfo.keyPath", String.class);
            final String caPath = config.getProperty("pemCertInfo.caPath", String.class);
            final String clientCertPath = config.getProperty("pemCertInfo.clientCertPath", String.class);
            final String serverConfigPath = serverConfigPath(version);

            container.withClasspathResourceMapping(CERT_BUNDLE, CONTAINER_CONFIG + "/" + CERT_BUNDLE, BindMode.READ_ONLY)
                    .withCopyFileToContainer(MountableFile.forClasspathResource(serverConfigPath + "/users"), CONTAINER_CONFIG + "/x-pack/users")
                    .withCopyFileToContainer(MountableFile.forClasspathResource(serverConfigPath + "/users_roles"), CONTAINER_CONFIG + "/x-pack/users_roles")
                    .withEnv("xpack.security.enabled", "true")
                    .withEnv("xpack.security.http.ssl.enabled", "true")
                    .withEnv("xpack.security.http.ssl.verification_mode", "none")
                    .withEnv("xpack.security.transport.ssl.enabled", "true")
                    .withEnv("xpack.security.transport.ssl.verification_mode", "none")
                    .withEnv("xpack.ssl.key",  containerCertPath(keyPath))
                    .withEnv("xpack.ssl.certificate",  containerCertPath(clientCertPath))
                    .withEnv("xpack.ssl.certificate_authorities", containerCertPath(caPath));

        } else {

            container.withEnv("xpack.security.enabled", "false")
                    .withEnv("xpack.security.http.ssl.enabled", "false")
                    .withEnv("xpack.security.transport.ssl.enabled", "false");

        }

        container.setWaitStrategy(new LogMessageWaitStrategy().withRegEx(".*Node.*started.*"));

    }
}
