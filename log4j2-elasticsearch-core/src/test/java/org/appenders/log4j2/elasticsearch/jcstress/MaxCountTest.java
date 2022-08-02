package org.appenders.log4j2.elasticsearch.jcstress;

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

import org.appenders.log4j2.elasticsearch.metrics.DefaultMetricsFactory;
import org.appenders.log4j2.elasticsearch.metrics.Metric;
import org.appenders.log4j2.elasticsearch.metrics.MetricConfigFactory;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.L_Result;

import java.util.Collections;

@JCStressTest
@Outcome.Outcomes({
        @Outcome(id="3", expect = Expect.ACCEPTABLE, desc="OK"),
})
@State
public class MaxCountTest {

    private static final long LONG_1 = 1L;
    private static final long LONG_2 = LONG_1 + 1;
    private static final long LONG_3 = LONG_1 + 2;

    private final Metric metric = new DefaultMetricsFactory(Collections.singletonList(MetricConfigFactory.createMaxConfig(true, "jcstress", true)))
            .createMetric("jcstress-test", "jcstress");

    @Actor
    public void storeMax1(L_Result r) {
        metric.store(LONG_1);
    }

    @Actor
    public void storeMax2(L_Result r) {
        metric.store(LONG_2);
    }

    @Actor
    public void storeMax3(L_Result r) {
        metric.store(LONG_3);
    }

    @Arbiter
    public void arbiter(L_Result r) {
         metric.accept((key, value) -> r.r1 = value);
    }

}
