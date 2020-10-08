package org.appenders.log4j2.elasticsearch;

/**
 * Allows to create client-specific administrative {@link Operation}s
 */
public abstract class SetupOperationFactory implements OperationFactory {

    /**
     * Index template setup
     *
     * @param indexTemplate {@link IndexTemplate} definition
     * @return {@link Operation} that executes given index template
     */
    public abstract Operation indexTemplate(IndexTemplate indexTemplate);

    /**
     * ILM policy setup
     *
     * @param ilmPolicy {@link ILMPolicy} definition
     * @return {@link Operation} that executes given ILM policy
     */
    public abstract Operation ilmPolicy(ILMPolicy ilmPolicy);

    /**
     * Handles unsupported {@link Operation}s
     *
     * @param opSource operation definition
     * @return throws by default
     */
    public Operation handleUnsupported(OpSource opSource) {
        throw new IllegalArgumentException(opSource.getClass().getSimpleName() + " is not supported");
    }

    /**
     * Dispatches given {@link OpSource} by {@link OpSource#getType()}.
     * <br>
     * Falls back to {@link #handleUnsupported(OpSource)} if none of supported types matches given {@link OpSource#getType()}
     *
     * @param opSource operation definition
     * @return If supported {@link Operation} that's ready to execute, throws otherwise
     */
    @Override
    public final Operation create(OpSource opSource) {

        final Operation operation;

        switch (opSource.getType()) {
            case IndexTemplate.TYPE_NAME : {
                operation = indexTemplate((IndexTemplate) opSource);
                break;
            }
            case ILMPolicy.TYPE_NAME: {
                operation = ilmPolicy((ILMPolicy) opSource);
                break;
            }
            default:
                operation = handleUnsupported(opSource);
        }

        return operation;

     }

}
