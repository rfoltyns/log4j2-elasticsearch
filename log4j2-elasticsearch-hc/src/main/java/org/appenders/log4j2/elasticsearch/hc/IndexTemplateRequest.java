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

import io.netty.buffer.ByteBuf;
import org.appenders.log4j2.elasticsearch.ByteBufItemSource;
import org.appenders.log4j2.elasticsearch.ItemSource;

/**
 * {@link ItemSource} based index template.
 * @deprecated As of 2.0, this class will be replaced with {@link GenericRequest}
 */
@Deprecated
public class IndexTemplateRequest implements Request {

    public static final String HTTP_METHOD_NAME = "PUT";
    protected final String templateName;
    protected final Object source;

    private IndexTemplateRequest(Builder builder) {
        this.templateName = builder.templateName;
        this.source = builder.source;
    }

    @Override
    public String getURI() {
        StringBuilder sb = new StringBuilder("_template/");
        sb.append(templateName);
        return sb.toString();
    }

    @Override
    public String getHttpMethodName() {
        return HTTP_METHOD_NAME;
    }

    @Override
    public ItemSource serialize() {
        return new ByteBufItemSource((ByteBuf) source, null);
    }

    public static class Builder {

        private String templateName;
        private Object source;

        public IndexTemplateRequest build() {
            return new IndexTemplateRequest(this);
        }

        public Builder withTemplateName(String templateName) {
            this.templateName = templateName;
            return this;
        }

        public Builder withSource(Object source) {
            this.source = source;
            return this;
        }
    }
}
