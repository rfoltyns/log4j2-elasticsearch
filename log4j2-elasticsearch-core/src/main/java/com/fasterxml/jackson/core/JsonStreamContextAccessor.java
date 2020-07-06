package com.fasterxml.jackson.core;

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

/**
 * Helper class to allow {@code com.fasterxml.jackson.core.JsonStreamContext} state access.
 */
public class JsonStreamContextAccessor {

    public static final int TYPE_ROOT = JsonStreamContext.TYPE_ROOT;

    public int getType(JsonStreamContext ctx) {
        return ctx._type;
    }

}
