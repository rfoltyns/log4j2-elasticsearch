package org.appenders.log4j2.elasticsearch;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2018 Rafal Foltynski
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


import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.appenders.log4j2.elasticsearch.spi.BatchEmitterServiceProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ServiceLoader;

import static org.appenders.log4j2.elasticsearch.AppenderRefFailoverPolicyTest.createTestFailoverPolicy;
import static org.appenders.log4j2.elasticsearch.BatchDeliveryTest.createTestObjectFactoryBuilder;
import static org.appenders.log4j2.elasticsearch.BulkEmitterTest.LARGE_TEST_INTERVAL;
import static org.appenders.log4j2.elasticsearch.BulkEmitterTest.TEST_BATCH_SIZE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.spy;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        ServiceLoader.class,
        BatchEmitterServiceProvider.class
})
public class BatchEmitterServiceProviderTest {

    @Mock
    private ServiceLoader<BatchEmitterFactory> mockServiceLoader;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp()
    {
        PowerMockito.mockStatic(ServiceLoader.class);
        Mockito.when(ServiceLoader.load(any(Class.class), any(ClassLoader.class))).thenReturn(mockServiceLoader);
    }

    @Test
    public void throwsExceptionWhenNoServiceWasFound() {

        // given
        BatchEmitterServiceProvider serviceProvider = spy(new BatchEmitterServiceProvider());

        Iterator iterator = mock(Iterator.class);
        when(iterator.hasNext()).thenReturn(false);

        when(mockServiceLoader.iterator()).thenReturn(iterator);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("No compatible BatchEmitter implementations");

        // when
        serviceProvider.createInstance(TEST_BATCH_SIZE,
                LARGE_TEST_INTERVAL,
                createTestObjectFactoryBuilder().build(),
                createTestFailoverPolicy("testRefAppender", mock(Configuration.class))
        );

    }

    @Test
    public void succeedsWhenCompatibleFactoryWasFound() {

        // given
        BatchEmitterServiceProvider serviceProvider = spy(new BatchEmitterServiceProvider());

        Iterator<BatchEmitterFactory> iterator = new ArrayList<BatchEmitterFactory>() {{
            add(new TestBatchEmitterFactory());
        }}.iterator();

        when(mockServiceLoader.iterator()).thenReturn(iterator);

        // when
        BatchEmitter emitter = createWithTestValues(serviceProvider);

        // then
        Assert.assertNotNull(emitter);

    }

    @Test
    public void throwsWhenFoundFactoryWasIncompatible() {

        // given
        BatchEmitterServiceProvider serviceProvider = spy(new BatchEmitterServiceProvider());
        TestBatchEmitterFactory emitterFactory = mock(TestBatchEmitterFactory.class);
        Iterator<BatchEmitterFactory> iterator = new ArrayList<BatchEmitterFactory>() {{
            add(emitterFactory);
        }}.iterator();

        when(mockServiceLoader.iterator()).thenReturn(iterator);

        when(emitterFactory.accepts(Matchers.<Class<TestHttpObjectFactory>>any())).thenReturn(false);

        expectedException.expect(ConfigurationException.class);
        expectedException.expectMessage("No compatible BatchEmitter implementations");

        // when
        createWithTestValues(serviceProvider);

    }

    private BatchEmitter createWithTestValues(BatchEmitterServiceProvider serviceProvider) {
        return serviceProvider.createInstance(TEST_BATCH_SIZE,
                LARGE_TEST_INTERVAL,
                createTestObjectFactoryBuilder().build(),
                createTestFailoverPolicy("testRefAppender", mock(Configuration.class))
        );
    }

}
