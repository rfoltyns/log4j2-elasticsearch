package org.appenders.log4j2.elasticsearch.metrics;

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

public class MetricConfig {

    private final MetricType type;
    private final String name;
    private final boolean enabled;
    private final boolean reset;

    public MetricConfig(final MetricType type,
                        final String name,
                        final boolean enabled,
                        final boolean reset) {
        this.type = type;
        this.name = name;
        this.enabled = enabled;
        this.reset = reset;
    }

    public MetricType getMetricType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isReset() {
        return reset;
    }

}
