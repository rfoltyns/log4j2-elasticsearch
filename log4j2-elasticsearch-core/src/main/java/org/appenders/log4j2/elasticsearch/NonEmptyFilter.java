package org.appenders.log4j2.elasticsearch;

import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;

public class NonEmptyFilter implements VirtualPropertyFilter {

    private static final Logger LOG = InternalLogging.getLogger();

    /**
     * Allows to determine inclusion based on presence and length of given value.
     *
     * @param fieldName Name to be logged on exclusion
     * @param resolvedValue result of {@link ValueResolver#resolve(VirtualProperty)}
     *
     * @return <i>true</i>, if {@code resolvedValue} is not null and it's length is greater than 0, <i>false</i> otherwise
     */
    @Override
    public final boolean isIncluded(String fieldName, String resolvedValue) {

        if (resolvedValue == null) {
            LOG.debug("VirtualProperty with excluded. Value was null. Name: {}", fieldName);
            return false;
        }

        if (resolvedValue.isEmpty()) {
            LOG.debug("VirtualProperty with excluded. Value was empty. Name: {}", fieldName);
            return false;
        }

        return true;
    }

}