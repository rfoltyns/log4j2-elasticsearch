package org.appenders.log4j2.elasticsearch.bulkprocessor.load;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2018 Rafal Foltynski
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import org.apache.logging.log4j.core.LoggerContext;
import org.appenders.log4j2.elasticsearch.AsyncBatchDelivery;
import org.appenders.log4j2.elasticsearch.BatchDelivery;
import org.appenders.log4j2.elasticsearch.CertInfo;
import org.appenders.log4j2.elasticsearch.ElasticsearchAppender;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.JacksonJsonLayout;
import org.appenders.log4j2.elasticsearch.NoopIndexNameFormatter;
import org.appenders.log4j2.elasticsearch.bulkprocessor.BasicCredentials;
import org.appenders.log4j2.elasticsearch.bulkprocessor.BulkProcessorObjectFactory;
import org.appenders.log4j2.elasticsearch.bulkprocessor.ClientSetting;
import org.appenders.log4j2.elasticsearch.bulkprocessor.ClientSettings;
import org.appenders.log4j2.elasticsearch.bulkprocessor.JKSCertInfo;
import org.appenders.log4j2.elasticsearch.bulkprocessor.ShieldAuth;
import org.appenders.log4j2.elasticsearch.load.SmokeTestBase;

import static org.appenders.core.util.PropertiesUtil.getInt;

public class SmokeTest extends SmokeTestBase {

    @Override
    public ElasticsearchAppender.Builder createElasticsearchAppenderBuilder(boolean messageOnly, boolean buffered, boolean secured) {

        BulkProcessorObjectFactory.Builder builder = BulkProcessorObjectFactory.newBuilder()
                .withServerUris("tcp://localhost:9300");

        ClientSetting clusterName = ClientSetting.newBuilder()
                .withName("cluster.name")
                .withValue(System.getProperty("clusterName"))
                .build();

        ClientSettings clientSettings = ClientSettings.newBuilder()
                .withClientSettings(clusterName).build();

        builder.withClientSettings(clientSettings);

        if (secured) {
            CertInfo certInfo = JKSCertInfo.newBuilder()
                    .withKeystorePath(System.getProperty("jksCertInfo.keystorePath"))
                    .withKeystorePassword(System.getProperty("jksCertInfo.keystorePassword"))
                    .withTruststorePath(System.getProperty("jksCertInfo.truststorePath"))
                    .withTruststorePassword(System.getProperty("jksCertInfo.truststorePassword"))
                    .build();

            BasicCredentials credentials = BasicCredentials.newBuilder()
                    .withUsername("admin")
                    .withPassword("changeme")
                    .build();

            ShieldAuth auth = ShieldAuth.newBuilder()
                    .withCertInfo(certInfo)
                    .withCredentials(credentials)
                    .build();

            builder.withAuth(auth);
        }

        IndexTemplate indexTemplate = IndexTemplate.newBuilder()
                .withName("log4j2-elasticsearch2-bulkprocessor-index-template")
                .withPath("classpath:indexTemplate.json")
                .build();

        BulkProcessorObjectFactory bulkProcessorObjectFactory = builder.build();

        BatchDelivery asyncBatchDelivery = AsyncBatchDelivery.newBuilder()
                .withClientObjectFactory(bulkProcessorObjectFactory)
                .withBatchSize(getInt("smokeTest.batchSize", 10000))
                .withDeliveryInterval(1000)
                .withSetupOpSources(indexTemplate)
                .build();

        NoopIndexNameFormatter indexNameFormatter = NoopIndexNameFormatter.newBuilder()
                .withIndexName("log4j2_test_es2")
                .build();

        JacksonJsonLayout jacksonJsonLayout = JacksonJsonLayout.newBuilder()
                .setConfiguration(LoggerContext.getContext(false).getConfiguration()).build();

        return ElasticsearchAppender.newBuilder()
                .withName(getConfig().getProperty("appenderName", String.class))
                .withLayout(jacksonJsonLayout)
                .withBatchDelivery(asyncBatchDelivery)
                .withIndexNameFormatter(indexNameFormatter)
                .withIgnoreExceptions(false);

    }

}
