package com.fasterxml.jackson.core.json;

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

import static com.fasterxml.jackson.core.JsonStreamContextAccessor.TYPE_ROOT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;

public class JsonWriteContextAccessorTest {

    @Test
    public void resetResetsCurrentContextImmediatelyIfContextHasNoParent() {

        // given
        JsonWriteContextAccessor ctxAccess = new JsonWriteContextAccessor();
        JsonWriteContext context = new JsonWriteContext(TYPE_ROOT, null, null);

        // when
        JsonWriteContext ctxAfterReset = ctxAccess.reset(context);

        // then
        assertSame(context, ctxAfterReset);
        assertEquals(ctxAccess.getType(ctxAfterReset), TYPE_ROOT);

    }

    @Test
    public void resetResetsAndReturnsRootContextIfContextHasParent() {

        // given
        JsonWriteContextAccessor ctxAccess = new JsonWriteContextAccessor();
        JsonWriteContext grandpaContext = new JsonWriteContext(TYPE_ROOT, null, null);
        JsonWriteContext parentContext = grandpaContext.createChildObjectContext();
        JsonWriteContext childContext = parentContext.createChildObjectContext();

        int childContextType = ctxAccess.getType(childContext);

        assertNotEquals(ctxAccess.getType(childContext), TYPE_ROOT);

        // when
        JsonWriteContext ctxAfterReset = ctxAccess.reset(childContext);

        // then
        assertSame(grandpaContext, ctxAfterReset);
        assertEquals(TYPE_ROOT, ctxAccess.getType(ctxAfterReset));
        assertEquals(childContextType, ctxAccess.getType(childContext));

    }

}