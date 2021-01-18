package org.appenders.log4j2.elasticsearch;

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

public class JacksonMixIn {

    private final Class targetClass;
    private final Class mixInClass;

    protected JacksonMixIn(Class targetClass, Class mixInClass) {
        this.targetClass = targetClass;
        this.mixInClass = mixInClass;
    }

    public Class getTargetClass() {
        return targetClass;
    }

    public Class getMixInClass() {
        return mixInClass;
    }

    public static class Builder {

        private String targetClassName;
        private String mixInClassName;

        public JacksonMixIn build() {

            Class targetClass = loadClass(targetClassName, "targetClass");
            Class mixInClass = loadClass(mixInClassName, "mixInClass");

            return new JacksonMixIn(targetClass, mixInClass);

        }

        private Class loadClass(String className, String argName) {

            if (className == null) {
                throw new IllegalArgumentException(String.format("No %s provided for %s", argName, JacksonMixIn.class.getName()));
            }

            try {
                // TODO: Extract to util class
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(String.format("Cannot load %s: %s for %s", argName, className, JacksonMixIn.class.getName()));
            }

        }

        public Builder withTargetClass(String targetClass) {
            this.targetClassName = targetClass;
            return this;
        }

        public Builder withMixInClass(String mixInClass) {
            this.mixInClassName = mixInClass;
            return this;
        }

    }

}
