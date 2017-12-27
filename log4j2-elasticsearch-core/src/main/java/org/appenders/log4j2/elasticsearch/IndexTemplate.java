package org.appenders.log4j2.elasticsearch;

import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginValue;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;

@Plugin(name = IndexTemplate.PLUGIN_NAME, category = Node.CATEGORY, elementType = IndexTemplate.ELEMENT_TYPE, printObject = true)
public class IndexTemplate {

    public static final String PLUGIN_NAME = "IndexTemplate";
    public static final String ELEMENT_TYPE = "indexTemplate";

    private String name;
    private String source;

    protected IndexTemplate(String name, String source) {
        this.name = name;
        this.source = source;
    }

    public String getName() {
        return name;
    }

    public String getSource() {
        return this.source;
    }

    @PluginBuilderFactory
    public static IndexTemplate.Builder newBuilder() {
        return new IndexTemplate.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<IndexTemplate> {

        public static final String CLASSPATH_PREFIX = "classpath:";
        @PluginAttribute("name")
        @Required
        private String name;

        @PluginAttribute("path")
        private String path;

        @PluginValue("sourceString")
        private String source;

        @Override
        public IndexTemplate build() {
            if (name == null) {
                throw new ConfigurationException("No name provided for IndexTemplate");
            }
            if ((path == null && source == null) || (path != null && source != null)) {
                throw new ConfigurationException("Either path or source have to be provided for IndexTemplate");
            }
            return new IndexTemplate(name, loadSource());
        }

        private String loadSource() {

            if (source != null) {
                return source;
            }

            if (path.contains(CLASSPATH_PREFIX)) {
                return loadClasspathResource();
            }

            return loadFileSystemResource();

        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withPath(String path) {
            this.path = path;
            return this;
        }

        public Builder withSource(String source) {
            this.source = source;
            return this;
        }

        private String loadClasspathResource() {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        ClassLoader.getSystemClassLoader().getResourceAsStream(
                                path.replace(CLASSPATH_PREFIX, "")),
                        "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                    sb.append('\n');
                }
                return sb.toString();
            } catch (Exception e) {
                throw new ConfigurationException(e.getMessage(), e);
            }
        }

        private String loadFileSystemResource() {
            try {
                return new String(Files.readAllBytes(Paths.get(path)));
            } catch (IOException e){
                throw new ConfigurationException(e.getMessage(), e);
            }
        }

    }

}