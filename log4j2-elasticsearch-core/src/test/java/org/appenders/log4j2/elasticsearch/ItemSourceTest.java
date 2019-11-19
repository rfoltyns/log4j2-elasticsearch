package org.appenders.log4j2.elasticsearch;

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

import org.junit.Test;

import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.ignoreStubs;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ItemSourceTest {

    @Test
    public void defaultReleaseHasNoSideEffects() {

        // given
        ItemSource itemSource = spy(new ItemSource() {
            @Override
            public Object getSource() {
                return null;
            }
        });

        // get StubInfo in with 'thenCallRealMethod' - this will allow ignoreStubs to mark it
        // as ignorable on verifyNoMoreInteractions
        doCallRealMethod().when(itemSource).release();

        // when
        itemSource.release();

        // then
        ignoreStubs(itemSource);
        verifyNoMoreInteractions(itemSource);

    }

}
