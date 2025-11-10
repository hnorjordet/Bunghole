/*******************************************************************************
 * Copyright (c) 2008 - 2025 Håvard Nørjordet.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Håvard Nørjordet - initial API and implementation
 *******************************************************************************/

package com.maxprograms.bunghole.ai;

import com.maxprograms.bunghole.Configuration;

/**
 * Factory for creating AI provider instances
 */
public class AIProviderFactory {

    public enum Provider {
        CLAUDE("Claude", "claude"),
        OPENAI("OpenAI", "openai");

        private final String displayName;
        private final String configKey;

        Provider(String displayName, String configKey) {
            this.displayName = displayName;
            this.configKey = configKey;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getConfigKey() {
            return configKey;
        }

        public static Provider fromString(String value) {
            for (Provider p : values()) {
                if (p.name().equalsIgnoreCase(value) ||
                    p.configKey.equalsIgnoreCase(value) ||
                    p.displayName.equalsIgnoreCase(value)) {
                    return p;
                }
            }
            return CLAUDE; // Default
        }
    }

    /**
     * Get the configured AI provider
     * @return AI provider instance
     */
    public static AIProvider getProvider() {
        Configuration config = Configuration.getInstance();
        String providerName = config.getAIProvider();
        Provider provider = Provider.fromString(providerName);

        return getProvider(provider);
    }

    /**
     * Get a specific AI provider
     * @param provider the provider type
     * @return AI provider instance
     */
    public static AIProvider getProvider(Provider provider) {
        Configuration config = Configuration.getInstance();

        switch (provider) {
            case OPENAI:
                String openaiKey = config.getOpenAIKey();
                if (openaiKey == null || openaiKey.isEmpty()) {
                    openaiKey = System.getenv("OPENAI_API_KEY");
                }
                String openaiModel = config.getOpenAIModel();
                return new OpenAIService(openaiKey, openaiModel);

            case CLAUDE:
            default:
                String claudeKey = config.getClaudeKey();
                if (claudeKey == null || claudeKey.isEmpty()) {
                    claudeKey = System.getenv("CLAUDE_API_KEY");
                }
                String claudeModel = config.getModelName();
                return new ClaudeAIServiceAdapter(claudeKey, claudeModel);
        }
    }

    /**
     * Get all available providers
     * @return array of providers
     */
    public static Provider[] getAvailableProviders() {
        return Provider.values();
    }

    /**
     * Check if a provider is configured
     * @param provider the provider to check
     * @return true if configured with API key
     */
    public static boolean isProviderConfigured(Provider provider) {
        AIProvider aiProvider = getProvider(provider);
        return aiProvider.isConfigured();
    }
}
