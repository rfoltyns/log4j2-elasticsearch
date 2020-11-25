package org.appenders.log4j2.elasticsearch.hc;

import io.netty.buffer.ByteBuf;
import org.appenders.log4j2.elasticsearch.EmptyItemSourceFactory;
import org.appenders.log4j2.elasticsearch.ILMPolicy;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.OpSource;
import org.appenders.log4j2.elasticsearch.Operation;
import org.appenders.log4j2.elasticsearch.OperationFactory;
import org.appenders.log4j2.elasticsearch.SetupStep;
import org.appenders.log4j2.elasticsearch.SkippingSetupStepChain;
import org.appenders.log4j2.elasticsearch.StepProcessor;
import org.appenders.log4j2.elasticsearch.ValueResolver;

import java.util.Arrays;

import static org.appenders.log4j2.elasticsearch.hc.CreateBootstrapIndex.BOOTSTRAP_TEMPLATE;

public class ILMPolicySetupOp implements OperationFactory {

    protected final StepProcessor<SetupStep<Request, Response>> stepProcessor;
    protected final ValueResolver valueResolver;
    protected final EmptyItemSourceFactory itemSourceFactory;

    private final ByteBufItemSourceWriter writer = new ByteBufItemSourceWriter();

    public ILMPolicySetupOp(StepProcessor<SetupStep<Request, Response>> stepProcessor, ValueResolver valueResolver, EmptyItemSourceFactory itemSourceFactory) {
        this.stepProcessor = stepProcessor;
        this.valueResolver = valueResolver;
        this.itemSourceFactory = itemSourceFactory;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends OpSource> Operation create(T opSource) {

        ILMPolicy ilmPolicy = (ILMPolicy) opSource;

        SetupStep<Request, Response> checkBootstrapIndex =
                new CheckBootstrapIndex(ilmPolicy.getRolloverAlias());

        String bootstrapIndexRequestBody = String.format(BOOTSTRAP_TEMPLATE, ilmPolicy.getRolloverAlias());
        SetupStep<Request, Response> createBootstrapIndex = new CreateBootstrapIndex(
                ilmPolicy.getRolloverAlias(),
                writer.write(itemSourceFactory.createEmptySource(), bootstrapIndexRequestBody.getBytes()));

        String ilmPolicyRequestBody = valueResolver.resolve(ilmPolicy.getSource());
        SetupStep<Request, Response> updateIlmPolicy = new PutILMPolicy(
                ilmPolicy.getName(),
                writer.write(itemSourceFactory.createEmptySource(), ilmPolicyRequestBody.getBytes()));

        return new SkippingSetupStepChain<>(Arrays.asList(checkBootstrapIndex, createBootstrapIndex, updateIlmPolicy), stepProcessor);

    }

}
