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

import com.fasterxml.jackson.core.JsonStreamContextAccessor;

import static com.fasterxml.jackson.core.JsonStreamContextAccessor.TYPE_ROOT;

/**
 * Helper class to allow {@code com.fasterxml.jackson.core.json.JsonWriteContext} state access.
 */
public class JsonWriteContextAccessor {

    private final JsonStreamContextAccessor streamCtxAccess = new JsonStreamContextAccessor();

    public JsonWriteContext reset(JsonWriteContext ctx) {

        if (ctx.getParent() == null) {
            return ctx.reset(TYPE_ROOT);
        }

        // Write context may be left unclosed, so..
        JsonWriteContext parent = ctx.getParent();
        while (parent.getParent() != null) {
            parent = parent.getParent();
        }

        return parent.reset(TYPE_ROOT);
    }

    int getType(JsonWriteContext ctx) {
        return streamCtxAccess.getType(ctx);
    }

}
