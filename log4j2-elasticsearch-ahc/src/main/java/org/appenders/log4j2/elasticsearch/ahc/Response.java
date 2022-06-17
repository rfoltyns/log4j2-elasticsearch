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

public interface Response {

    boolean isSucceeded();

    int getResponseCode();

    String getErrorMessage();

    /**
     * Allows to calculate actual response code based on current state
     *
     * @param responseCode original response code
     * @return this
     */
    Response withResponseCode(int responseCode);

    /**
     * Allows to construct a contextual error message based on given input and current state
     *
     * @param errorMessage original error message
     * @return this
     */
    Response withErrorMessage(String errorMessage);

}
