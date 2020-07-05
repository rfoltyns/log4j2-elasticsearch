package org.appenders.core.logging;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2020 Rafal Foltynski
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class LoggerTest {

    private static final String EXPECTED_MESSAGE = "Not implemented";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void errorThrowsByDefault() {

        // given
        Logger logger = new Logger() {};

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage(EXPECTED_MESSAGE);

        // when
        logger.error("test");

    }

    @Test
    public void warnThrowsByDefault() {

        // given
        Logger logger = new Logger() {};

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage(EXPECTED_MESSAGE);

        // when
        logger.warn("test");

    }

    @Test
    public void infoThrowsByDefault() {

        // given
        Logger logger = new Logger() {};

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage(EXPECTED_MESSAGE);

        // when
        logger.info("test");

    }

    @Test
    public void debugThrowsByDefault() {

        // given
        Logger logger = new Logger() {};

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage(EXPECTED_MESSAGE);

        // when
        logger.debug("test");

    }

    @Test
    public void traceThrowsByDefault() {

        // given
        Logger logger = new Logger() {};

        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage(EXPECTED_MESSAGE);

        // when
        logger.trace("test");

    }

}
