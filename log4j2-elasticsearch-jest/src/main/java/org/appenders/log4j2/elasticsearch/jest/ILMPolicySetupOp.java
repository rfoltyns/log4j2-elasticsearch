package org.appenders.log4j2.elasticsearch.jest;

import io.searchbox.client.JestResult;
import org.appenders.log4j2.elasticsearch.ILMPolicy;
import org.appenders.log4j2.elasticsearch.OpSource;
import org.appenders.log4j2.elasticsearch.Operation;
import org.appenders.log4j2.elasticsearch.OperationFactory;
import org.appenders.log4j2.elasticsearch.SetupStep;
import org.appenders.log4j2.elasticsearch.SkippingSetupStepChain;
import org.appenders.log4j2.elasticsearch.StepProcessor;
import org.appenders.log4j2.elasticsearch.ValueResolver;

import java.util.Arrays;

public class ILMPolicySetupOp implements OperationFactory {

    private final StepProcessor<SetupStep<GenericJestRequest, JestResult>> stepProcessor;
    private final ValueResolver valueResolver;

    public ILMPolicySetupOp(
            StepProcessor<SetupStep<GenericJestRequest, JestResult>> stepProcessor,
            ValueResolver valueResolver) {
        this.stepProcessor = stepProcessor;
        this.valueResolver = valueResolver;
    }


    @Override
    public <T extends OpSource> Operation create(T opSource) {

        ILMPolicy ilmPolicy = (ILMPolicy)opSource;

        SetupStep<GenericJestRequest, JestResult>
                hasBootstrapIndex = new CheckBootstrapIndex(ilmPolicy.getRolloverAlias());

        SetupStep<GenericJestRequest, JestResult>
                createBootstrapIndex = new CreateBootstrapIndex(ilmPolicy.getRolloverAlias());

        SetupStep<GenericJestRequest, JestResult> updateIlmPolicy = new PutILMPolicy(
                ilmPolicy.getName(),
                valueResolver.resolve(ilmPolicy.getSource()));

        return new SkippingSetupStepChain<>(Arrays.asList(hasBootstrapIndex, createBootstrapIndex, updateIlmPolicy), stepProcessor);

    }
}
