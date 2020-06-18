package org.appenders.core.util;

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


import static org.appenders.core.logging.InternalLogging.getLogger;

public class PropertiesUtil {

    private PropertiesUtil() {
        // static only
    }

    public static Integer getInt(String propertyName, int defaultValue) {

        String value = System.getProperty(propertyName);

        if (value == null || value.trim().isEmpty()) {
            logMissingValue(propertyName, defaultValue);
            return defaultValue;
        }

        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            logParsingError(propertyName, defaultValue, e);
            return defaultValue;
        }
    }

    private static void logMissingValue(String propertyName, int defaultValue) {
        getLogger().warn(
                "Property {} not found. Returning default: {}",
                propertyName,
                defaultValue
        );
    }

    private static void logParsingError(String propertyName, int defaultValue, Exception e) {
        getLogger().error(
                "{} {} while parsing {}. Returning default: {}",
                e.getClass().getSimpleName(),
                e.getMessage(),
                propertyName,
                defaultValue
        );
    }
}