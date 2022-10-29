package org.appenders.log4j2.elasticsearch.util;

public class UriUtil {

    private UriUtil() {
        // static
    }

    public static void appendQueryParam(final StringBuilder uriBuilder, final String name, final String value) {

        if (value == null || value.isEmpty()) {
            return;
        }

        final String current = uriBuilder.toString();

        if (!current.contains("?")) {
            uriBuilder.append("?").append(name).append("=").append(value);
            return;
        }

        uriBuilder.append("&").append(name).append("=").append(value);

    }

    public static void appendPath(final StringBuilder uriBuilder, final String name) {

        if (name == null || name.isEmpty()) {
            return;
        }

        final String current = uriBuilder.toString();

        if (!current.endsWith("/")) {
            uriBuilder.append("/").append(name);
            return;
        }

        uriBuilder.append(name);

    }
}
