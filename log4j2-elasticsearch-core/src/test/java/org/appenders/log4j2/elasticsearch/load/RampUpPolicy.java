package org.appenders.log4j2.elasticsearch.load;

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

public class RampUpPolicy implements ThrottlingPolicy {

    private final int max;
    private final double percentOnResize;

    public RampUpPolicy(final int max, final double resizeFactor) {

        if (resizeFactor > 1) {
            throw new IllegalArgumentException("Resize factor too aggressive for ramp up policy: " + resizeFactor);
        }

        this.max = max;
        this.percentOnResize = max * resizeFactor / max;

    }

    @Override
    public double throttle(final double currentLoad, final int limitPerSec) {

        if (currentLoad == 0.0d) {
            return currentLoad;
        }

        if (currentLoad * limitPerSec > max && currentLoad > 1.05d) {
            return 1.0d + percentOnResize;
        } else if (currentLoad < 0.95d){
            return currentLoad + (currentLoad * percentOnResize);
        } else {
            return 1.0d;
        }

    }

}
