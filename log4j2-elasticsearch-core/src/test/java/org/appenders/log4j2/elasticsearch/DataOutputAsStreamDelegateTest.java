package org.appenders.log4j2.elasticsearch;

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

import org.junit.Test;

import java.io.DataOutput;
import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DataOutputAsStreamDelegateTest {

    @Test
    public void canReplaceDelegate() throws IOException {

        // given
        DataOutput dataOutput1 = mock(DataOutput.class);
        DataOutputAsStreamDelegate dataOutputDelegate = new DataOutputAsStreamDelegate(dataOutput1);
        dataOutputDelegate.write(1);

        // when
        DataOutput dataOutput2 = mock(DataOutput.class);
        dataOutputDelegate.setDelegate(dataOutput2);
        dataOutputDelegate.write(1);

        // then
        verify(dataOutput1).write(1);
        verify(dataOutput2).write(1);

    }

    @Test
    public void writeIntDelegates() throws IOException {

        // given
        DataOutput dataOutput1 = mock(DataOutput.class);
        DataOutputAsStreamDelegate dataOutputDelegate = new DataOutputAsStreamDelegate(dataOutput1);

        // when
        dataOutputDelegate.write(1);

        // then
        verify(dataOutput1).write(1);

    }

    @Test
    public void writeBytesDelegate() throws IOException {

        // given
        DataOutput dataOutput1 = mock(DataOutput.class);
        DataOutputAsStreamDelegate dataOutputDelegate = new DataOutputAsStreamDelegate(dataOutput1);

        // when
        byte[] bytes = {1};
        dataOutputDelegate.write(bytes);

        // then
        verify(dataOutput1).write(bytes);

    }

    @Test
    public void writeBytesWithOffsetDelegates() throws IOException {

        // given
        DataOutput dataOutput1 = mock(DataOutput.class);
        DataOutputAsStreamDelegate dataOutputDelegate = new DataOutputAsStreamDelegate(dataOutput1);

        // when
        byte[] bytes = {1};
        dataOutputDelegate.write(bytes, 2, bytes.length);

        // then
        verify(dataOutput1).write(bytes, 2, bytes.length);

    }

}
