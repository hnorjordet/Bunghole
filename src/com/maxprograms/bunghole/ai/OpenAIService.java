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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONObject;

import com.maxprograms.bunghole.Configuration;

/**
 * Service for communicating with OpenAI API (ChatGPT)
 * Used for improving uncertain alignments
 */
public class OpenAIService implements AIProvider {

    private static final Logger logger = System.getLogger(OpenAIService.class.getName());

    private String apiKey;
    private String model;
    private double inputTokenPrice;   // per million tokens
    private double outputTokenPrice;  // per million tokens

    public OpenAIService(String apiKey) {
        this(apiKey, null);
    }

    public OpenAIService(String apiKey, String model) {
        this.apiKey = apiKey;

        // Load from configuration or use defaults
        Configuration config = Configuration.getInstance();
        this.model = model != null ? model : config.getOpenAIModel();
        this.inputTokenPrice = config.getOpenAIInputPrice();
        this.outputTokenPrice = config.getOpenAIOutputPrice();
    }

    @Override
    public String getProviderName() {
        return "OpenAI";
    }

    @Override
    public String getModelName() {
        return model;
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    @Override
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
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

        if (!isConfigured()) {
            throw new IllegalStateException("OpenAI API key not configured");
        }

        // Build the prompt
        String prompt = buildAlignmentPrompt(sourceSegments, targetSegments, sourceLang, targetLang);

        // Create request
        JSONObject request = new JSONObject();
        request.put("model", model);
        request.put("temperature", 0.3);
        request.put("max_tokens", 4000);

        // Build messages array
        JSONArray messages = new JSONArray();

        // System message
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are an expert translation quality analyzer. Analyze alignment quality between source and target segments.");
        messages.put(systemMessage);

        // User message
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.put(userMessage);

        request.put("messages", messages);

        // Make API call
        String apiUrl = Configuration.getInstance().getOpenAIApiUrl();

        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            // Set request headers
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);

            // Send request
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = request.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Read response
            int responseCode = conn.getResponseCode();

            if (responseCode == 200) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                }

                JSONObject jsonResponse = new JSONObject(response.toString());

                // Extract the assistant's message
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices.length() > 0) {
                    JSONObject firstChoice = choices.getJSONObject(0);
                    JSONObject message = firstChoice.getJSONObject("message");
                    String content = message.getString("content");

                    // Parse the JSON response from the assistant
                    return parseAIResponse(content);
                }

                throw new Exception("No response from OpenAI API");

            } else {
                // Read error response
                StringBuilder errorResponse = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line);
                    }
                }

                logger.log(Level.ERROR, "OpenAI API error: " + errorResponse.toString());
                throw new Exception("OpenAI API error: " + responseCode + " - " + errorResponse.toString());
            }

        } finally {
            conn.disconnect();
        }
    }

    private String buildAlignmentPrompt(String[] sourceSegments, String[] targetSegments,
                                       String sourceLang, String targetLang) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Analyze these ").append(sourceLang).append(" to ").append(targetLang)
              .append(" segment alignments. For each pair, assess if the translation is accurate and if the segments are properly aligned.\n\n");

        prompt.append("Respond with a JSON object containing:\n");
        prompt.append("{\n");
        prompt.append("  \"segments\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"index\": 0,\n");
        prompt.append("      \"confidence\": 0-100,  // 0=terrible, 100=perfect\n");
        prompt.append("      \"issues\": \"description of any issues\",\n");
        prompt.append("      \"isUncertain\": true/false\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");

        prompt.append("Segment pairs to analyze:\n\n");

        for (int i = 0; i < Math.min(sourceSegments.length, targetSegments.length); i++) {
            prompt.append("[").append(i).append("]\n");
            prompt.append("Source: ").append(sourceSegments[i]).append("\n");
            prompt.append("Target: ").append(targetSegments[i]).append("\n\n");
        }

        return prompt.toString();
    }

    private JSONObject parseAIResponse(String content) throws Exception {
        // Try to extract JSON from the response
        // OpenAI might wrap it in markdown code blocks
        content = content.trim();

        // Remove markdown code blocks if present
        if (content.startsWith("```json")) {
            content = content.substring(7);
        } else if (content.startsWith("```")) {
            content = content.substring(3);
        }

        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }

        content = content.trim();

        try {
            return new JSONObject(content);
        } catch (Exception e) {
            logger.log(Level.ERROR, "Failed to parse AI response as JSON: " + content);
            throw new Exception("Invalid JSON response from AI: " + e.getMessage());
        }
    }

    @Override
    public CostEstimate estimateCost(int totalSegments, int uncertainSegments) {
        // Estimate tokens for OpenAI
        // GPT models use similar token estimation to Claude
        int avgCharsPerSegment = 100; // Conservative estimate
        double tokensPerChar = 0.25; // Rough approximation

        // Input: system prompt + user prompt with segments
        int systemPromptTokens = 50;
        int instructionTokens = 200;
        int segmentTokens = (int) (totalSegments * avgCharsPerSegment * 2 * tokensPerChar); // source + target
        int estimatedInputTokens = systemPromptTokens + instructionTokens + segmentTokens;

        // Output: JSON response with analysis
        int tokensPerAnalysis = 50; // per segment analysis
        int estimatedOutputTokens = totalSegments * tokensPerAnalysis;

        // Calculate cost
        double inputCost = (estimatedInputTokens / 1_000_000.0) * inputTokenPrice;
        double outputCost = (estimatedOutputTokens / 1_000_000.0) * outputTokenPrice;
        double totalCost = inputCost + outputCost;

        return new CostEstimate(
            estimatedInputTokens,
            estimatedOutputTokens,
            totalCost,
            getProviderName(),
            getModelName()
        );
    }
}
