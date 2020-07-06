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

import com.fasterxml.jackson.core.util.BufferRecycler;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertSame;

public class ReusableIOContextTest {

    @Test
    public void canReassignIOContextSourceReference() {

        // then
        OutputStream initialSourceRef = new ByteArrayOutputStream();
        ReusableIOContext ctx = createDefaultTestIOContext(initialSourceRef);

        OutputStream expectedSourceRef = new ByteArrayOutputStream();

        // when
        ctx.setSourceReference(expectedSourceRef);

        // then
        Object result = ctx.getSourceReference();
        assertSame(expectedSourceRef, result);

    }

    @NotNull
    public ReusableIOContext createDefaultTestIOContext(OutputStream initialSourceRef) {
        return new ReusableIOContext(
                new BufferRecycler(), initialSourceRef, false);
    }

}
