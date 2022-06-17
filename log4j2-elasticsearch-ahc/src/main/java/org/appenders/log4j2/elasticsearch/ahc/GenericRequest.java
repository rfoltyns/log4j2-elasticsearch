package org.appenders.log4j2.elasticsearch.ahc;

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

import org.appenders.log4j2.elasticsearch.ItemSource;

/**
 * Generic HTTP request with {@link ItemSource} payload
 */
public class GenericRequest implements Request {

    private final String httpMethodName;
    private final String uri;
    private final ItemSource itemSource;

    public GenericRequest(final String httpMethodName,
                          final String uri,
                          final ItemSource itemSource) {
        this.httpMethodName = httpMethodName;
        this.uri = uri;
        this.itemSource = itemSource;
    }

    @Override
    public String getURI() {
        return uri;
    }

    @Override
    public String getHttpMethodName() {
        return httpMethodName;
    }

    @Override
    public ItemSource serialize() {
        return itemSource;
    }

}
