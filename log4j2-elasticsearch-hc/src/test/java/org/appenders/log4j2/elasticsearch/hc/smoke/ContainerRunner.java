package org.appenders.log4j2.elasticsearch.hc.smoke;

import org.appenders.log4j2.elasticsearch.smoke.TestConfig;
import org.appenders.log4j2.elasticsearch.util.Version;
import org.appenders.log4j2.elasticsearch.util.VersionUtil;
import org.testcontainers.containers.Network;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static org.appenders.core.logging.InternalLogging.getLogger;

public class ContainerRunner {

    private static final DockerImageName ELASTICSEARCH_IMAGE = DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch");

    private final ThreadLocal<ContainerCallback<List<String>>> resultRefs = new ThreadLocal<>();
    // TODO: move to thread local?
    private final List<ElasticsearchContainer> containers = new ArrayList<>();

    private final TestConfig config;

    public ContainerRunner(TestConfig config) {
        this.config = config;
    }

    public List<String> start() {
        return startContainers();
    }

    protected List<String> startContainers() {

        if (resultRefs.get() == null) {

            final ContainerCallback<List<String>> callback = new ContainerCallback<>();
            resultRefs.set(callback);

            final Thread startInBackground = new StartContainerTask(30000, callback);
            startInBackground.start();

        }

        return startContainers(
                config.getProperty("containers.startup.maxChecks", Integer.class),
                TimeUnit.SECONDS.toMillis(config.getProperty("containers.startup.timeoutSeconds", Integer.class)));

    }

    private List<String> startContainers(int retries, long timeout) {

        long retryInterval = timeout / retries;

        while (resultRefs.get().completed() && retries > 0) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(retryInterval));
            getLogger().info("Containers not started yet. Timing out in {}s", retries * TimeUnit.MILLISECONDS.toSeconds(retryInterval));
        }

        if (resultRefs.get().completed()) {
            throw new IllegalStateException("Unable to start containers on time");
        }

        return resultRefs.get().getResult();

    }

    private ElasticsearchContainer startContainer() {

        final Version apiVersion = VersionUtil.parse(config.getProperty("api.version", String.class));
        final boolean secure = config.getProperty("secure", Boolean.class);

        final ElasticsearchContainer container = new ElasticsearchContainer(ELASTICSEARCH_IMAGE.withTag(apiVersion.toString()))
                .withReuse(false)
                .withLogConsumer(outputFrame -> getLogger().warn(outputFrame.getUtf8String()))
                .withExposedPorts(9200)
                .withNetwork(Network.SHARED);

        resolveConfigurer(apiVersion).configure(container, apiVersion, config);
        container.start();

        return container;
    }

    private ElasticsearchContainerConfigurer resolveConfigurer(Version apiVersion) {

        if (apiVersion.lowerThan("5.0.0")) {
            throw new UnsupportedOperationException("Elasticsearch container version not supported: " + apiVersion);
        }

        if (apiVersion.lowerThan("6.0.0")) {
            return new Elasticsearch5Configurer();
        }

        if (apiVersion.lowerThan("7.0.0")) {
            return new Elasticsearch6Configurer();
        }

        if (apiVersion.lowerThan("8.0.0")) {
            return new Elasticsearch7Configurer();
        }

        throw new UnsupportedOperationException("Elasticsearch container version not supported: " + apiVersion);

    }

    public void stop() {

        for (ElasticsearchContainer container : containers) {
            container.stop();
        }

    }

    private class StartContainerTask extends Thread {

        private final int timeout;

        private final ContainerCallback<List<String>> callback;

        public StartContainerTask(final int timeout, final ContainerCallback<List<String>> callback) {
            this.timeout = timeout;
            this.callback = callback;
        }

        @Override
        public void run() {

            final ElasticsearchContainer container = startContainer();
            containers.add(container);

            callback.completed(Collections.singletonList(container.getHttpHostAddress()));

        }

        public int getTimeout() {
            return timeout;
        }

    }

    private static class ContainerCallback<T> {

        private volatile T result;

        void completed(T result) {
            this.result = result;
        }

        public boolean completed() {
            return result == null;
        }

        public T getResult() {
            return result;
        }
    }

}
