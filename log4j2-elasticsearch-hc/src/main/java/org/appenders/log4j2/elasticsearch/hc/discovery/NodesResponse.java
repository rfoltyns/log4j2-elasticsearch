package org.appenders.log4j2.elasticsearch.hc.discovery;

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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.appenders.log4j2.elasticsearch.hc.Response;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NodesResponse implements Response {

    private int responseCode;
    private String errorMessage;

    private final Map<String, NodeInfo> nodes;

    public NodesResponse(@JsonProperty("nodes") Map<String, NodeInfo> nodes) {
        this.nodes = nodes;
    }

    @Override
    public boolean isSucceeded() {
        return nodes != null;
    }

    @Override
    public int getResponseCode() {
        return responseCode;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public NodesResponse withResponseCode(int responseCode) {
        this.responseCode = responseCode;
        return this;
    }

    @Override
    public NodesResponse withErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public Map<String, NodeInfo> getNodes() {
        return nodes;
    }

}
