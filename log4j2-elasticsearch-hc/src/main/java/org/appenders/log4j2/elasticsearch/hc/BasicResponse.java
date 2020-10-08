package org.appenders.log4j2.elasticsearch.hc;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2019 Rafal Foltynski
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

public class BasicResponse implements Response {

    private int responseCode;
    private String errorMessage;

    /**
     * @return true, if {@link #responseCode} is higher than 0 and less than 400, false otherwise
     */
    @Override
    public boolean isSucceeded() {
        return responseCode > 0 && responseCode < 400;
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
    public BasicResponse withResponseCode(int responseCode) {
        this.responseCode = responseCode;
        return this;
    }

    @Override
    public BasicResponse withErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }
}
