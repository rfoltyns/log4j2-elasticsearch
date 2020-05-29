package org.appenders.log4j2.elasticsearch;

public interface VirtualPropertyFilter {

    /**
     * Allows to determine inclusion based on given field name and/or value
     *
     * @param fieldName {@link VirtualProperty#getName()}
     * @param value result of {@link ValueResolver#resolve(VirtualProperty)}
     *
     * @return <i>true</i>, if {@link VirtualProperty} should be included by {@link VirtualPropertiesWriter}, <i>false</i> otherwise
     */
    boolean isIncluded(String fieldName, String value);

}
