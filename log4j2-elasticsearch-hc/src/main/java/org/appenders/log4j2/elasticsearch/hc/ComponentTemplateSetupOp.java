package org.appenders.log4j2.elasticsearch.hc;

import org.appenders.log4j2.elasticsearch.ComponentTemplate;
import org.appenders.log4j2.elasticsearch.EmptyItemSourceFactory;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.OpSource;
import org.appenders.log4j2.elasticsearch.Operation;
import org.appenders.log4j2.elasticsearch.OperationFactory;
import org.appenders.log4j2.elasticsearch.SetupStep;
import org.appenders.log4j2.elasticsearch.SkippingSetupStepChain;
import org.appenders.log4j2.elasticsearch.StepProcessor;
import org.appenders.log4j2.elasticsearch.ValueResolver;

import java.util.Collections;

/**
 * Component template setup. See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-component-template.html">Component Templates</a>
 */
public class ComponentTemplateSetupOp implements OperationFactory {

    protected final StepProcessor<SetupStep<Request, Response>> stepProcessor;
    protected final ValueResolver valueResolver;
    protected final EmptyItemSourceFactory itemSourceFactory;

    private final ByteBufItemSourceWriter writer = new ByteBufItemSourceWriter();

    public ComponentTemplateSetupOp(
            StepProcessor<SetupStep<Request, Response>> stepProcessor,
            ValueResolver valueResolver,
            EmptyItemSourceFactory itemSourceFactory) {
        this.stepProcessor = stepProcessor;
        this.valueResolver = valueResolver;
        this.itemSourceFactory = itemSourceFactory;
    }

    /**
     * @param opSource {@link ComponentTemplate} definition
     * @return {@link Operation} that executes given component template
     */
    @Override
    public <T extends OpSource> Operation create(T opSource) {

        ComponentTemplate componentTemplate = (ComponentTemplate) opSource;

        ItemSource emptySource = itemSourceFactory.createEmptySource();

        final SetupStep<Request, Response> putComponentTemplate = new PutComponentTemplate(
                componentTemplate.getName(),
                writer.write(emptySource, valueResolver.resolve(componentTemplate.getSource()).getBytes())
        );

        return new SkippingSetupStepChain<>(Collections.singletonList(putComponentTemplate), stepProcessor);

    }

}
