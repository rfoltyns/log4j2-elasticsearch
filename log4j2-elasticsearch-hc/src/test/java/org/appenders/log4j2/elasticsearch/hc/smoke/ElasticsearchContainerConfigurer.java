package org.appenders.log4j2.elasticsearch.hc.smoke;

import org.appenders.log4j2.elasticsearch.smoke.TestConfig;
import org.appenders.log4j2.elasticsearch.util.Version;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

public interface ElasticsearchContainerConfigurer {

    String CONTAINER_CONFIG = "/usr/share/elasticsearch/config";
    String SERVER_CONFIG = "server-config";
    String CERT_BUNDLE = "certificate-bundle";

    void configure(ElasticsearchContainer container, Version version, TestConfig config);

    default String serverConfigPath(Version apiVersion) {
        return SERVER_CONFIG + "/elasticsearch" + apiVersion.major();
    }

    default String containerCertPath(String fullPath) {
        return fullPath.substring(fullPath.indexOf(CERT_BUNDLE));
    }

}
