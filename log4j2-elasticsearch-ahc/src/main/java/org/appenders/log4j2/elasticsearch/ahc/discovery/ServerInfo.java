package org.appenders.log4j2.elasticsearch.ahc.discovery;

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

import java.net.MalformedURLException;
import java.net.URL;

public class ServerInfo {

    private final String resolvedAddress;

    /**
     * @param url MUST be a valid {@code java.net.URL}
     */
    public ServerInfo(final String url) {

        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Can't resolve address: " + url, e);
        }

        this.resolvedAddress = url;

    }

    public String getResolvedAddress() {
        return resolvedAddress;
    }

}
