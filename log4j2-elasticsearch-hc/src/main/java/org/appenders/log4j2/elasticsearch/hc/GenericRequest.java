package org.appenders.log4j2.elasticsearch.hc;

import org.appenders.log4j2.elasticsearch.ItemSource;

/**
 * Generic HTTP request with {@link ItemSource} payload
 */
public class GenericRequest implements Request {

    private final String httpMethodName;
    private final String uri;
    private final ItemSource itemSource;

    public GenericRequest(String httpMethodName, String uri, ItemSource itemSource) {
        this.httpMethodName = httpMethodName;
        this.uri = uri;
        this.itemSource = itemSource;
    }

    @Override
    public String getURI() {
        return uri;
    }

    @Override
    public String getHttpMethodName() {
        return httpMethodName;
    }

    @Override
    public ItemSource serialize() {
        return itemSource;
    }

}
