package org.appenders.log4j2.elasticsearch.bulkprocessor;

import org.appenders.log4j2.elasticsearch.ILMPolicy;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.Operation;
import org.appenders.log4j2.elasticsearch.SetupOperationFactory;
import org.appenders.log4j2.elasticsearch.SkippingSetupStepChain;
import org.appenders.log4j2.elasticsearch.StepProcessor;
import org.appenders.log4j2.elasticsearch.ValueResolver;
import org.elasticsearch.action.support.master.AcknowledgedResponse;

import java.util.Collections;

/**
 * {@inheritDoc}
 */
public class BulkProcessorSetupOperationFactory extends SetupOperationFactory {

    protected final ValueResolver valueResolver;
    protected final StepProcessor<BulkProcessorSetupStep<AcknowledgedResponse>> stepProcessor;

    public BulkProcessorSetupOperationFactory(
            StepProcessor<BulkProcessorSetupStep<AcknowledgedResponse>> stepProcessor,
            ValueResolver valueResolver) {
        this.valueResolver = valueResolver;
        this.stepProcessor = stepProcessor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Operation indexTemplate(IndexTemplate indexTemplate) {
        String resolved = valueResolver.resolve(indexTemplate.getSource());
        PutIndexTemplate putIndexTemplate = new PutIndexTemplate(indexTemplate.getName(), resolved.getBytes());
        return new SkippingSetupStepChain<>(Collections.singletonList(putIndexTemplate), stepProcessor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Operation ilmPolicy(ILMPolicy ilmPolicy) {
        throw new IllegalArgumentException(ilmPolicy.getClass().getSimpleName() + " is not supported");
    }

}
