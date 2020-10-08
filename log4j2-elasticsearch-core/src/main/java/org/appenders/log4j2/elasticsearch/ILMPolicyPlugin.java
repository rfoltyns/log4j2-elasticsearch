package org.appenders.log4j2.elasticsearch;

import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginValue;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

/**
 * {@inheritDoc}
 *
 * Extension for Log4j2
 */
@Plugin(name = ILMPolicyPlugin.PLUGIN_NAME, category = Node.CATEGORY, elementType = "setupOperation", printObject = true)
public class ILMPolicyPlugin extends ILMPolicy {

    public static final String PLUGIN_NAME = "ILMPolicy";

    /**
     * {@inheritDoc}
     */
    protected ILMPolicyPlugin(String policyName, String rolloverAlias, String source) {
        super(policyName, rolloverAlias, source);
    }

    @PluginBuilderFactory
    public static ILMPolicyPlugin.Builder newBuilder() {
        return new ILMPolicyPlugin.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<ILMPolicyPlugin> {

        @PluginAttribute("name")
        @Required
        private String name;

        @PluginAttribute("rolloverAlias")
        @Required
        private String rolloverAlias;

        @PluginAttribute("path")
        private String path;

        @PluginValue("sourceString")
        private String source;

        public ILMPolicyPlugin build() {

            if (name == null) {
                throw new ConfigurationException("No name provided for " + PLUGIN_NAME);
            }

            if (rolloverAlias == null) {
                throw new ConfigurationException("No rolloverAlias provided for " + PLUGIN_NAME);
            }

            if ((path == null && source == null) || (path != null && source != null)) {
                throw new ConfigurationException("Either path or source have to be provided for " + ILMPolicyPlugin.class.getSimpleName());
            }

            return new ILMPolicyPlugin(name, rolloverAlias, loadSource());
        }

        private String loadSource() {

            if (source != null) {
                return source;
            }

            return ResourceUtil.loadResource(path);
        }

        /**
         * @param name ILM policy name
         * @return this
         */
        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        /**
         * @param path ILM policy document resource path.
         *             MUST be resolvable by {@link ResourceUtil#loadResource(String)}.
         *             Resource MAY contain placeholders resolvable by {@link ValueResolver}.
         * @return this
         */
        public Builder withPath(String path) {
            this.path = path;
            return this;
        }


        /**
         * @param rolloverAlias Index rollover alias
         * @return this
         */
        public Builder withRolloverAlias(String rolloverAlias) {
            this.rolloverAlias = rolloverAlias;
            return this;
        }

        /**
         * @param source ILM policy document. MAY contain placeholders resolvable by {@link ValueResolver}
         * @return this
         */
        public Builder withSource(String source) {
            this.source = source;
            return this;
        }

    }

}
