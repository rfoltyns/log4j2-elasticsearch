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

/**
 * Wraps failed item delivery parameters.
 *
 * Consider this class <i>private</i> - design may change before code is stabilized.
 */
public class FailedItemInfo {

    private final String targetName;

    public FailedItemInfo(String targetName) {
        this.targetName = targetName;
    }

    /**
     * @return item target name
     */
    public String getTargetName() {
        return targetName;
    }

}
