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

package com.maxprograms.bunghole;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Application configuration management
 * Loads configuration from properties file or environment variables
 */
public class Configuration {

    private static Configuration instance;
    private Properties properties;

    // Default values
    private static final int DEFAULT_PORT = 8040;
    private static final int DEFAULT_MIN_THREADS = 3;
    private static final int DEFAULT_MAX_THREADS = 10;
    private static final int DEFAULT_THREAD_TIMEOUT = 20;
    private static final int DEFAULT_QUEUE_SIZE = 100;
    private static final String DEFAULT_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String DEFAULT_MODEL = "claude-sonnet-4-20250514";
    private static final double DEFAULT_INPUT_PRICE = 3.0;
    private static final double DEFAULT_OUTPUT_PRICE = 15.0;
    private static final String DEFAULT_BIND_ADDRESS = "127.0.0.1";

    private Configuration() {
        properties = new Properties();
        loadConfiguration();
    }

    public static synchronized Configuration getInstance() {
        if (instance == null) {
            instance = new Configuration();
        }
        return instance;
    }

    private void loadConfiguration() {
        // Try to load from config file
        String configFile = System.getProperty("bunghole.config", "config.properties");
        try (InputStream input = new FileInputStream(configFile)) {
            properties.load(input);
        } catch (IOException e) {
            // Config file not found, use defaults and environment variables
        }
    }

    /**
     * Get server port
     */
    public int getServerPort() {
        String port = getProperty("server.port", String.valueOf(DEFAULT_PORT));
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException e) {
            return DEFAULT_PORT;
        }
    }

    /**
     * Get server bind address (should be 127.0.0.1 for security)
     */
    public String getBindAddress() {
        return getProperty("server.bindAddress", DEFAULT_BIND_ADDRESS);
    }

    /**
     * Get minimum thread pool size
     */
    public int getMinThreads() {
        String threads = getProperty("server.minThreads", String.valueOf(DEFAULT_MIN_THREADS));
        try {
            return Integer.parseInt(threads);
        } catch (NumberFormatException e) {
            return DEFAULT_MIN_THREADS;
        }
    }

    /**
     * Get maximum thread pool size
     */
    public int getMaxThreads() {
        String threads = getProperty("server.maxThreads", String.valueOf(DEFAULT_MAX_THREADS));
        try {
            return Integer.parseInt(threads);
        } catch (NumberFormatException e) {
            return DEFAULT_MAX_THREADS;
        }
    }

    /**
     * Get thread timeout in seconds
     */
    public int getThreadTimeout() {
        String timeout = getProperty("server.threadTimeout", String.valueOf(DEFAULT_THREAD_TIMEOUT));
        try {
            return Integer.parseInt(timeout);
        } catch (NumberFormatException e) {
            return DEFAULT_THREAD_TIMEOUT;
        }
    }

    /**
     * Get thread queue size
     */
    public int getQueueSize() {
        String size = getProperty("server.queueSize", String.valueOf(DEFAULT_QUEUE_SIZE));
        try {
            return Integer.parseInt(size);
        } catch (NumberFormatException e) {
            return DEFAULT_QUEUE_SIZE;
        }
    }

    /**
     * Get Claude API URL
     */
    public String getApiUrl() {
        return getProperty("claude.apiUrl", DEFAULT_API_URL);
    }

    /**
     * Get Claude model name
     */
    public String getModelName() {
        return getProperty("claude.model", DEFAULT_MODEL);
    }

    /**
     * Get input token price (per million)
     */
    public double getInputTokenPrice() {
        String price = getProperty("claude.inputPrice", String.valueOf(DEFAULT_INPUT_PRICE));
        try {
            return Double.parseDouble(price);
        } catch (NumberFormatException e) {
            return DEFAULT_INPUT_PRICE;
        }
    }

    /**
     * Get output token price (per million)
     */
    public double getOutputTokenPrice() {
        String price = getProperty("claude.outputPrice", String.valueOf(DEFAULT_OUTPUT_PRICE));
        try {
            return Double.parseDouble(price);
        } catch (NumberFormatException e) {
            return DEFAULT_OUTPUT_PRICE;
        }
    }

    /**
     * Get property with fallback to environment variable
     */
    private String getProperty(String key, String defaultValue) {
        // First check system properties
        String value = System.getProperty(key);
        if (value != null) {
            return value;
        }

        // Then check loaded properties file
        value = properties.getProperty(key);
        if (value != null) {
            return value;
        }

        // Finally check environment variables (convert dots to underscores)
        String envKey = key.replace('.', '_').toUpperCase();
        value = System.getenv(envKey);
        if (value != null) {
            return value;
        }

        return defaultValue;
    }

    /**
     * Check if running in development mode
     */
    public boolean isDevelopmentMode() {
        String mode = getProperty("bunghole.mode", "production");
        return "development".equalsIgnoreCase(mode);
    }

    /**
     * Get log level
     */
    public String getLogLevel() {
        return getProperty("logging.level", "INFO");
    }

    /**
     * Get AI provider selection (claude or openai)
     */
    public String getAIProvider() {
        return getProperty("ai.provider", "claude");
    }

    /**
     * Get OpenAI API key
     */
    public String getOpenAIKey() {
        return getProperty("openai.apiKey", "");
    }

    /**
     * Get OpenAI model name
     */
    public String getOpenAIModel() {
        return getProperty("openai.model", "gpt-4-turbo-preview");
    }

    /**
     * Get OpenAI API URL
     */
    public String getOpenAIApiUrl() {
        return getProperty("openai.apiUrl", "https://api.openai.com/v1/chat/completions");
    }

    /**
     * Get OpenAI input token price (per million)
     */
    public double getOpenAIInputPrice() {
        String price = getProperty("openai.inputPrice", "10.0");
        try {
            return Double.parseDouble(price);
        } catch (NumberFormatException e) {
            return 10.0;
        }
    }

    /**
     * Get OpenAI output token price (per million)
     */
    public double getOpenAIOutputPrice() {
        String price = getProperty("openai.outputPrice", "30.0");
        try {
            return Double.parseDouble(price);
        } catch (NumberFormatException e) {
            return 30.0;
        }
    }

    /**
     * Get Claude API key
     */
    public String getClaudeKey() {
        return getProperty("claude.apiKey", "");
    }
}
