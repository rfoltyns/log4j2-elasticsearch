package org.appenders.log4j2.elasticsearch;

import org.apache.logging.log4j.core.LogEvent;

public interface IndexNameFormatter {

    String ELEMENT_TYPE = "indexNameFormatter";

    String format(LogEvent logEvent);

}
