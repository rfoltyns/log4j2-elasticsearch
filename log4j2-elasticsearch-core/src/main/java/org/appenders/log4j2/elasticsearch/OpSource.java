package org.appenders.log4j2.elasticsearch;

/**
 * Defines input for {@link OperationFactory}
 */
public interface OpSource {

    /**
     * @return type name
     */
    String getType();

    /**
     * @return operation input
     */
    String getSource();

}
