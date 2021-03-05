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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Stream;

public class ResourceUtil {

    public static final String CLASSPATH_PREFIX = "classpath:";

    private ResourceUtil() {
        // static
    }

    /**
     * Loads resource from given URI.
     * <br>
     * If starts with {@code classpath:}, {@link ClassLoader#getSystemClassLoader()} will be used to locate resource.
     * <br>
     * Otherwise, {@link java.nio.file.Files} will be used to load a regular file.
     *
     * @param uri resource URI
     * @return resource content
     */
    public static String loadResource(final String uri) {

        if (uri == null) {
            throw new IllegalArgumentException("uri cannot be null");
        }

        if (uri.contains(CLASSPATH_PREFIX)) {
            return loadClasspathResource(uri);
        }

        return loadFileSystemResource(uri);

    }

    private static String loadClasspathResource(final String path) {
        try {
            String resourcePath = path.replace(CLASSPATH_PREFIX, "");

            InputStream resource = loadClasspathResource(resourcePath, getClassLoaders());
            if (resource == null) {
                throw new IllegalArgumentException("Requested classpath resource was null: " + path);
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private static InputStream loadClasspathResource(String resourcePath, ClassLoader... classLoaders) {
        for (ClassLoader classLoader : classLoaders) {
            final InputStream resource = classLoader.getResourceAsStream(resourcePath);
            if (resource != null) {
                return resource;
            }
        }
        return null;
    }

    private static ClassLoader[] getClassLoaders() {
        return Stream.of(ClassLoader.getSystemClassLoader(), Thread.currentThread().getContextClassLoader())
                .filter(Objects::nonNull)
                .toArray(ClassLoader[]::new);
    }

    private static String loadFileSystemResource(String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path)));
        } catch (IOException e){
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

}
