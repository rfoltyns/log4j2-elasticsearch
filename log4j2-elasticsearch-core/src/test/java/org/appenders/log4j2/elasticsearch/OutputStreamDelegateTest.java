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

import java.io.IOException;
import java.io.OutputStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class OutputStreamDelegateTest {

    @Test
    public void canReplaceDelegate() throws IOException {

        // given
        OutputStream os1 = mock(OutputStream.class);
        OutputStreamDelegate outputStreamDelegate = new OutputStreamDelegate(os1);
        outputStreamDelegate.write(1);

        // when
        OutputStream os2 = mock(OutputStream.class);
        outputStreamDelegate.setDelegate(os2);
        outputStreamDelegate.write(1);

        // then
        verify(os1).write(1);
        verify(os2).write(1);

    }

    @Test
    public void writeIntDelegates() throws IOException {

        // given
        OutputStream os1 = mock(OutputStream.class);
        OutputStreamDelegate outputStreamDelegate = new OutputStreamDelegate(os1);

        // when
        outputStreamDelegate.write(1);

        // then
        verify(os1).write(1);

    }

    @Test
    public void writeBytesDelegate() throws IOException {

        // given
        OutputStream os1 = mock(OutputStream.class);
        OutputStreamDelegate outputStreamDelegate = new OutputStreamDelegate(os1);

        // when
        byte[] bytes = {1};
        outputStreamDelegate.write(bytes);

        // then
        verify(os1).write(bytes);

    }

    @Test
    public void writeBytesWithOffsetDelegates() throws IOException {

        // given
        OutputStream os1 = mock(OutputStream.class);
        OutputStreamDelegate outputStreamDelegate = new OutputStreamDelegate(os1);

        // when
        byte[] bytes = {1};
        outputStreamDelegate.write(bytes, 2, bytes.length);

        // then
        verify(os1).write(bytes, 2, bytes.length);

    }

    @Test
    public void closeDelegates() throws IOException {

        // given
        OutputStream os1 = mock(OutputStream.class);
        OutputStreamDelegate outputStreamDelegate = new OutputStreamDelegate(os1);

        // when
        outputStreamDelegate.close();

        // then
        verify(os1).close();

    }

}
