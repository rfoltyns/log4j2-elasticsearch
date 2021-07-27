package org.appenders.log4j2.elasticsearch.util;

/*-
 * #%L
 * log4j2-elasticsearch
 * %%
 * Copyright (C) 2021 Rafal Foltynski
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

public class Version implements Comparable<Version> {

    private final String[] parts;

    public Version(final String[] parts) {
        this.parts = parts;
    }

    public int major() {
        return Integer.parseInt(parts[0]);
    }

    public boolean higherThan(final Version other) {
        return compareTo(other) > 0;
    }

    public boolean lowerThan(final Version other) {
        return compareTo(other) < 0;
    }

    public boolean higherThan(final String versionStr) {
        return higherThan(VersionUtil.parse(versionStr));
    }

    public boolean lowerThan(final String versionStr) {
        return lowerThan(VersionUtil.parse(versionStr));
    }

    @Override
    public int compareTo(final Version other) {

        final int comparableParts = Math.min(parts.length, other.parts.length);

        for (int index = 0; index < comparableParts; index++) {
            ensureNonNegativeInteger(parts[index]);
            ensureNonNegativeInteger(other.parts[index]);

            final Integer part = Integer.parseInt(parts[index]);
            final Integer otherPart = Integer.parseInt(other.parts[index]);

            final int compared = part.compareTo(otherPart);
            if (compared != 0) {
                return compared;
            }
        }

        // Don't care about alpha, GA, RELEASE etc. At least for now..
        return Integer.compare(parts.length, other.parts.length);

    }

    private void ensureNonNegativeInteger(final String part) {

        try {
            final int parsed = Integer.parseInt(part);
            if (parsed < 0) {
                throw new IllegalArgumentException("Version part cannot be negative: " + part);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a number: " + part);
        }

    }

    @Override
    public String toString() {
        return String.join(".", parts);
    }

}
