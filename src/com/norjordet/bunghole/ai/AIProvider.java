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

package com.norjordet.bunghole.ai;

import org.json.JSONObject;

/**
 * Interface for AI service providers (Claude, OpenAI, etc.)
 */
public interface AIProvider {

    /**
     * Get the provider name
     * @return provider name (e.g., "Claude", "OpenAI")
     */
    String getProviderName();

    /**
     * Get the model name being used
     * @return model name (e.g., "claude-sonnet-4", "gpt-4")
     */
    String getModelName();

    /**
     * Check if the provider is configured with an API key
     * @return true if API key is set
     */
    boolean isConfigured();

    /**
     * Set the API key for this provider
     * @param apiKey the API key
     */
    void setApiKey(String apiKey);

    /**
     * Analyze alignment quality and suggest improvements
     * @param sourceSegments array of source text segments
     * @param targetSegments array of target text segments
     * @param sourceLang source language code
     * @param targetLang target language code
     * @return JSON response with analysis results
     * @throws Exception if API call fails
     */
    JSONObject analyzeAlignment(
        String[] sourceSegments,
        String[] targetSegments,
        String sourceLang,
        String targetLang
    ) throws Exception;

    /**
     * Estimate the cost of analyzing alignment
     * @param totalSegments number of segment pairs
     * @param uncertainSegments number of uncertain segments
     * @return cost estimate object
     */
    CostEstimate estimateCost(int totalSegments, int uncertainSegments);

    /**
     * Get input token price per million tokens
     * @return price in USD
     */
    double getInputTokenPrice();

    /**
     * Get output token price per million tokens
     * @return price in USD
     */
    double getOutputTokenPrice();
}
