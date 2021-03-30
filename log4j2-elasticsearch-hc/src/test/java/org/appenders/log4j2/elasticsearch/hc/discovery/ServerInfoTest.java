package org.appenders.log4j2.elasticsearch.hc.discovery;

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

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ServerInfoTest {

    @Test
    public void throwsOnInvalidUrl() {

        // given
        String invalidUrl = "notvalid";

        // when
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new ServerInfo(invalidUrl));

        // then
        assertThat(exception.getMessage(), is("Can't resolve address: " + invalidUrl));

    }

    @Test
    public void doesNotThrowOnHttpUrl() {

        // given
        String validUrl = "http://localhost";

        // when
        ServerInfo serverInfo = new ServerInfo(validUrl);

        // then
        assertEquals(validUrl, serverInfo.getResolvedAddress());

    }

}
