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

/**
 * ILM policy definition. Used as an input to {@link OperationFactory} to create client-specific ILM policy setup implementations.
 */
public class ILMPolicy implements OpSource {

    public static final String TYPE_NAME = "ILMPolicy";

    private final String name;
    private final String rolloverAlias;
    private final boolean createBootstrapIndex;
    private final String source;

    /**
     * @param name ILM policy name
     * @param rolloverAlias index rollover alias
     * @param source ILM policy document
     * @deprecated As of 1.7, this method will be removed. Use {@link #ILMPolicy(String, String, boolean, String)} instead.
     */
    @Deprecated
    public ILMPolicy(final String name, final String rolloverAlias, final String source) {
        this(name, rolloverAlias, true, source);
    }

    /**
     * @param name ILM policy name
     * @param rolloverAlias index rollover alias
     * @param createBootstrapIndex should bootstrap index be created or not
     * @param source ILM policy document
     */
    public ILMPolicy(final String name, final String rolloverAlias, final boolean createBootstrapIndex, final String source) {
        this.name = name;
        this.rolloverAlias = rolloverAlias;
        this.createBootstrapIndex = createBootstrapIndex;
        this.source = source;
    }

    /**
     * @return {@link #TYPE_NAME}
     */
    @Override
    public final String getType() {
        return TYPE_NAME;
    }

    /**
     * @return ILM policy name
     */
    public String getName() {
        return name;
    }

    /**
     * @return index rollover alias
     */
    public String getRolloverAlias() {
        return this.rolloverAlias;
    }

    public boolean isCreateBootstrapIndex() {
        return createBootstrapIndex;
    }

    /**
     * @return ILM policy document
     */
    @Override
    public final String getSource() {
        return this.source;
    }

}
