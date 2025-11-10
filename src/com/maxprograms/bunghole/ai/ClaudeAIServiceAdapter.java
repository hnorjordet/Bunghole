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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.maxprograms.bunghole.Configuration;
import com.maxprograms.bunghole.alignment.AlignmentPair;

/**
 * Adapter for ClaudeAIService to implement AIProvider interface
 */
public class ClaudeAIServiceAdapter implements AIProvider {

    private ClaudeAIService claudeService;
    private String model;
    private double inputTokenPrice;
    private double outputTokenPrice;

    public ClaudeAIServiceAdapter(String apiKey) {
        this(apiKey, null);
    }

    public ClaudeAIServiceAdapter(String apiKey, String model) {
        Configuration config = Configuration.getInstance();
        this.model = model != null ? model : config.getModelName();
        this.inputTokenPrice = config.getInputTokenPrice();
        this.outputTokenPrice = config.getOutputTokenPrice();

        this.claudeService = new ClaudeAIService(apiKey, this.model);
    }

    @Override
    public String getProviderName() {
        return "Claude";
    }

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public boolean isConfigured() {
        // ClaudeAIService doesn't have an isConfigured method, so check if service exists
        return claudeService != null;
    }

    @Override
    public void setApiKey(String apiKey) {
        this.claudeService = new ClaudeAIService(apiKey, this.model);
    }

    @Override
    public double getInputTokenPrice() {
        return inputTokenPrice;
    }

    @Override
    public double getOutputTokenPrice() {
        return outputTokenPrice;
    }

    @Override
    public JSONObject analyzeAlignment(
        String[] sourceSegments,
        String[] targetSegments,
        String sourceLang,
        String targetLang
    ) throws Exception {

        // Convert arrays to lists for ClaudeAIService
        List<String> sourceList = Arrays.asList(sourceSegments);
        List<String> targetList = Arrays.asList(targetSegments);

        // For this simple adapter, we'll treat all pairs as uncertain
        // In a real implementation, this would be determined by confidence scores
        List<AlignmentPair> uncertainPairs = new ArrayList<>();
        for (int i = 0; i < Math.min(sourceSegments.length, targetSegments.length); i++) {
            List<Integer> sourceIndices = Arrays.asList(i);
            List<Integer> targetIndices = Arrays.asList(i);
            uncertainPairs.add(new AlignmentPair(sourceIndices, targetIndices, 0.5, ""));
        }

        // Call Claude service
        List<AlignmentPair> improvedPairs = claudeService.improveAlignment(
            sourceList,
            targetList,
            uncertainPairs
        );

        // Convert result to JSONObject format expected by AIProvider interface
        JSONObject result = new JSONObject();
        JSONArray pairs = new JSONArray();

        for (AlignmentPair pair : improvedPairs) {
            JSONObject pairJson = new JSONObject();
            pairJson.put("sourceIndices", new JSONArray(pair.getSourceIndices()));
            pairJson.put("targetIndices", new JSONArray(pair.getTargetIndices()));
            pairJson.put("confidence", pair.getConfidence());
            pairJson.put("note", pair.getNote());
            pairs.put(pairJson);
        }

        result.put("pairs", pairs);
        result.put("provider", getProviderName());
        result.put("model", getModelName());

        return result;
    }

    @Override
    public CostEstimate estimateCost(int totalSegments, int uncertainSegments) {
        // Estimate tokens based on segment count
        // Average segment length: 50 characters
        // Average tokens per character: 0.25
        int avgSegmentTokens = 50 * (int)(0.25 * 10) / 10; // ~12 tokens per segment

        // Input: All segments + system prompt
        int inputTokens = (totalSegments * avgSegmentTokens * 2) + 1000; // *2 for source+target

        // Output: Analysis results for uncertain segments
        int outputTokens = uncertainSegments * 50; // ~50 tokens per analysis

        // Calculate cost
        double estimatedCost =
            (inputTokens / 1_000_000.0 * inputTokenPrice) +
            (outputTokens / 1_000_000.0 * outputTokenPrice);

        return new CostEstimate(
            inputTokens,
            outputTokens,
            estimatedCost,
            getProviderName(),
            getModelName()
        );
    }
}
