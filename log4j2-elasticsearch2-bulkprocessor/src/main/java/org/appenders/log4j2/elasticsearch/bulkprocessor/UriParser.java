package org.appenders.log4j2.elasticsearch.bulkprocessor;

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



import org.apache.logging.log4j.core.config.ConfigurationException;

import java.net.URI;
import java.net.URISyntaxException;

// just a facade until I find something more convenient..
public class UriParser {

    public String getHost(String s) {
        try {
            return new URI(s).getHost();
        } catch (URISyntaxException e) {
            throw new ConfigurationException(e.getMessage());
        }
    }

    public int getPort(String serverUri) {
        try {
            return new URI(serverUri).getPort();
        } catch (URISyntaxException e) {
            throw new ConfigurationException(e.getMessage());
        }
    }
}
