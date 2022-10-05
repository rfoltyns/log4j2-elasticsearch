package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2022 Rafal Foltynski
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

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ILMPolicyTest {

    private static final String TEST_ILM_POLICY_NAME = "test-ilm-policy";
    private static final String TEST_SOURCE = "{}";
    private static final String TEST_ROLLOVER_ALIAS = "test-rollover-alias";

    @Test
    public void deprecatedConstructorCreatesBootstrapIndex() {

        // when
        final ILMPolicy policy = new ILMPolicy(TEST_ILM_POLICY_NAME, TEST_ROLLOVER_ALIAS, TEST_SOURCE);

        // then
        assertTrue(policy.isCreateBootstrapIndex());

    }

}
