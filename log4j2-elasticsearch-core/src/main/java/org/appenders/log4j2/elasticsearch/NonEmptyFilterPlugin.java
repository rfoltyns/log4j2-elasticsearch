package org.appenders.log4j2.elasticsearch;

import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;


@Plugin(
        name = NonEmptyFilterPlugin.PLUGIN_NAME,
        category = Node.CATEGORY,
        elementType = NonEmptyFilterPlugin.ELEMENT_TYPE,
        printObject = true
)
public class NonEmptyFilterPlugin extends NonEmptyFilter {

    public static final String PLUGIN_NAME = "NonEmptyFilter";
    public static final String ELEMENT_TYPE = "virtualPropertyFilter";

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new NonEmptyFilterPlugin.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<NonEmptyFilterPlugin> {

        @Override
        public NonEmptyFilterPlugin build() {
            return new NonEmptyFilterPlugin();
        }

    }
}