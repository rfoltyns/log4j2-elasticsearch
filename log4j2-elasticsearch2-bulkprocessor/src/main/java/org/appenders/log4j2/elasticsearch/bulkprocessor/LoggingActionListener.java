package org.appenders.log4j2.elasticsearch.bulkprocessor;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.master.AcknowledgedResponse;

import static org.appenders.core.logging.InternalLogging.getLogger;

public class LoggingActionListener<T extends AcknowledgedResponse> implements ActionListener<T> {

    private final String actionName;

    public LoggingActionListener(String actionName) {
        this.actionName = actionName;
    }

    @Override
    public void onResponse(T response) {
        getLogger().info("{}: success", actionName);
    }

    @Override
    public void onFailure(Throwable e) {
        getLogger().error("{}: failure", actionName, e);
    }

}
