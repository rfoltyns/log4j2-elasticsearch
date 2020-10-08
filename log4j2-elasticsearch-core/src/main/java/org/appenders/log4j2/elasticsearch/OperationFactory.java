package org.appenders.log4j2.elasticsearch;

/**
 * Allows to create {@link Operation} from {@link OpSource}
 */
public interface OperationFactory {

    <T extends OpSource> Operation create(T opSource);

}
