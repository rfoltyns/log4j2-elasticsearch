package org.appenders.log4j2.elasticsearch.bulkprocessor;

import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.OpSource;
import org.appenders.log4j2.elasticsearch.Operation;
import org.appenders.log4j2.elasticsearch.OperationFactory;
import org.appenders.log4j2.elasticsearch.SkippingSetupStepChain;
import org.appenders.log4j2.elasticsearch.StepProcessor;
import org.appenders.log4j2.elasticsearch.ValueResolver;
import org.elasticsearch.action.support.master.AcknowledgedResponse;

import java.util.Collections;

public class IndexTemplateSetupOp implements OperationFactory {

    private final StepProcessor<BulkProcessorSetupStep<AcknowledgedResponse>> stepProcessor;
    private final ValueResolver valueResolver;

    public IndexTemplateSetupOp(
            StepProcessor<BulkProcessorSetupStep<AcknowledgedResponse>> stepProcessor,
            ValueResolver valueResolver) {
        this.stepProcessor = stepProcessor;
        this.valueResolver = valueResolver;
    }

    @Override
    public <T extends OpSource> Operation create(T opSource) {
        IndexTemplate indexTemplate = (IndexTemplate)opSource;
        String resolved = valueResolver.resolve(indexTemplate.getSource());
        PutIndexTemplate putIndexTemplate = new PutIndexTemplate(indexTemplate.getName(), resolved.getBytes());
        return new SkippingSetupStepChain<>(Collections.singletonList(putIndexTemplate), stepProcessor);
    }
}
