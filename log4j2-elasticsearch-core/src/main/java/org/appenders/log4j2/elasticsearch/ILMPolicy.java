package org.appenders.log4j2.elasticsearch;

/**
 * ILM policy definition. Used as an input to {@link OperationFactory} to create client-specific ILM policy setup implementations.
 */
public class ILMPolicy implements OpSource {

    public static final String TYPE_NAME = "ILMPolicy";

    private final String name;
    private final String rolloverAlias;
    private final String source;

    /**
     * @param name ILM policy name
     * @param rolloverAlias index rollover alias
     * @param source ILM policy document
     */
    public ILMPolicy(String name, String rolloverAlias, String source) {
        this.name = name;
        this.rolloverAlias = rolloverAlias;
        this.source = source;
    }

    @Override
    public final String getType() {
        return TYPE_NAME;
    }

    /**
     * @return ILM policy name
     */
    public String getName() {
        return name;
    }

    /**
     * @return index rollover alias
     */
    public String getRolloverAlias() {
        return this.rolloverAlias;
    }

    /**
     * @return ILM policy document
     */
    @Override
    public final String getSource() {
        return this.source;
    }

}
