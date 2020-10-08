package org.appenders.log4j2.elasticsearch.jest;

import io.searchbox.action.AbstractAction;
import io.searchbox.action.GenericResultAbstractAction;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.ElasticsearchVersion;

/**
 * Extended, lightweight Jest request
 */
public abstract class GenericJestRequest extends GenericResultAbstractAction {

    private static final AbstractAction.Builder<GenericJestRequest, JestResult> EMPTY_BUILDER = new EmptyBuilder();
    private final String httpMethodName;

    protected GenericJestRequest(String httpMethodName, String source) {
        super(EMPTY_BUILDER);
        this.httpMethodName = httpMethodName;
        this.payload = source;
    }

    public abstract String buildURI();

    @Override
    protected final String buildURI(ElasticsearchVersion elasticsearchVersion) {
        return buildURI();
    }

    @Override
    public final String getRestMethodName() {
        return httpMethodName;
    }

    static class EmptyBuilder extends AbstractAction.Builder<GenericJestRequest, JestResult> {

        @Override
        public GenericJestRequest build() {
            throw new UnsupportedOperationException("No need to use builder. Create directly");
        }

    }
}

