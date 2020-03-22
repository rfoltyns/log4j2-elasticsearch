package org.appenders.log4j2.elasticsearch.failover;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2019 Rafal Foltynski
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

import org.apache.logging.log4j.core.config.ConfigurationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertNotNull;

public class Log4j2SingleKeySequenceSelectorTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void builderBuildsSuccessfully() {

        // given
        Log4j2SingleKeySequenceSelector.Builder builder = Log4j2SingleKeySequenceSelector.newBuilder()
                .withSequenceId(1);

        // when
        Log4j2SingleKeySequenceSelector keySequenceSelector = builder.build();

        // then
        assertNotNull(keySequenceSelector);

    }

    @Test
    public void builderThrowsOnSequenceIdLowerThanZero() {

        // given
        Log4j2SingleKeySequenceSelector.Builder builder = Log4j2SingleKeySequenceSelector.newBuilder()
                .withSequenceId(-1);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("sequenceId must be higher than 0");

        // when
        builder.build();

    }

    @Test
    public void builderThrowsOnSequenceIdEqualZero() {

        // given
        Log4j2SingleKeySequenceSelector.Builder builder = Log4j2SingleKeySequenceSelector.newBuilder()
                .withSequenceId(0);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("sequenceId must be higher than 0");

        // when
        builder.build();

    }

}
