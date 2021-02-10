package org.appenders.log4j2.elasticsearch;

import com.fasterxml.jackson.core.JsonFactory;
import org.appenders.st.jackson.SingleThreadJsonFactory;

public final class SingleThreadJsonFactoryProvider {

    public JsonFactory create() {
        return new SingleThreadJsonFactory();
    }

}
