package org.appenders.log4j2.elasticsearch;

import com.fasterxml.jackson.core.JsonFactory;
import org.appenders.st.jackson.SingleThreadJsonFactory;
import org.junit.Assert;
import org.junit.Test;

public class SingleThreadJsonFactoryProviderTest {

    @Test
    public void createsSingleThreadJsonFactory() {

        // given
        SingleThreadJsonFactoryProvider provider = new SingleThreadJsonFactoryProvider();

        // when
        JsonFactory jsonFactory = provider.create();

        // then
        Assert.assertTrue(jsonFactory instanceof SingleThreadJsonFactory);

    }

}