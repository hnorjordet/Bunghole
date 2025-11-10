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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.norjordet.bunghole.alignment.AlignmentPair;

/**
 * Service for communicating with Claude AI API
 * Used for improving uncertain alignments
 */
public class ClaudeAIService {

    private static final Logger logger = System.getLogger(ClaudeAIService.class.getName());
    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";

    private String apiKey;
    private String model;

    public ClaudeAIService(String apiKey) {
        this(apiKey, "claude-sonnet-4-20250514");
    }

    public ClaudeAIService(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    /**
     * Improve uncertain alignments using Claude AI
     */
    public List<AlignmentPair> improveAlignment(
        List<String> sourceSegments,
        List<String> targetSegments,
        List<AlignmentPair> uncertainPairs
    ) throws IOException {

        if (uncertainPairs.isEmpty()) {
            return new ArrayList<>();
        }

        logger.log(Level.INFO, "Sending {0} uncertain alignments to Claude AI", uncertainPairs.size());

        // Build prompt
        String prompt = PromptBuilder.buildAlignmentPrompt(
            sourceSegments,
            targetSegments,
            uncertainPairs
        );

        // Call Claude API
        JSONObject response = callClaudeAPI(prompt);

        // Parse response
        List<AlignmentPair> improvedPairs = parseClaudeResponse(response);

        logger.log(Level.INFO, "Received {0} improved alignments from Claude", improvedPairs.size());

        return improvedPairs;
    }

    /**
     * Call Claude API with a prompt
     */
    private JSONObject callClaudeAPI(String prompt) throws IOException {
        URL url = new URL(CLAUDE_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            // Set request properties
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("x-api-key", apiKey);
            conn.setRequestProperty("anthropic-version", API_VERSION);
            conn.setDoOutput(true);

            // Build request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);
            requestBody.put("max_tokens", 4000);

            JSONArray messages = new JSONArray();
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);
            messages.put(message);
            requestBody.put("messages", messages);

            // Send request
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Read response
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                // Read error response
                String errorMsg = readErrorStream(conn);
                throw new IOException("Claude API returned error " + responseCode + ": " + errorMsg);
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }

            return new JSONObject(response.toString());

        } finally {
            conn.disconnect();
        }
    }

    private String readErrorStream(HttpURLConnection conn) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            return "Unknown error";
        }
    }

    /**
     * Parse Claude's response and extract alignment pairs
     */
    private List<AlignmentPair> parseClaudeResponse(JSONObject response) {
        List<AlignmentPair> pairs = new ArrayList<>();

        try {
            JSONArray content = response.getJSONArray("content");
            String text = content.getJSONObject(0).getString("text");

            // Extract JSON from response (might be wrapped in markdown)
            String jsonText = extractJSON(text);
            JSONObject alignmentData = new JSONObject(jsonText);

            JSONArray alignments = alignmentData.getJSONArray("alignments");

            for (int i = 0; i < alignments.length(); i++) {
                JSONObject align = alignments.getJSONObject(i);

                List<Integer> sourceIndices = jsonArrayToList(align.getJSONArray("source"));
                List<Integer> targetIndices = jsonArrayToList(align.getJSONArray("target"));
                double confidence = align.getDouble("confidence");
                String note = align.optString("note", "AI-improved");

                AlignmentPair pair = new AlignmentPair(sourceIndices, targetIndices, confidence, note);
                pair.setAiReviewed(true); // Mark as AI-reviewed
                pairs.add(pair);
            }

        } catch (Exception e) {
            logger.log(Level.ERROR, "Error parsing Claude response", e);
        }

        return pairs;
    }

    /**
     * Extract JSON from text (handles markdown code blocks)
     */
    private String extractJSON(String text) {
        // Try to find JSON in markdown code blocks
        int start = text.indexOf("```json");
        if (start != -1) {
            start = text.indexOf('\n', start) + 1;
            int end = text.indexOf("```", start);
            if (end != -1) {
                return text.substring(start, end).trim();
            }
        }

        // Try to find JSON object directly
        start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }

        // Last resort: return the whole text
        return text;
    }

    private List<Integer> jsonArrayToList(JSONArray array) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add(array.getInt(i));
        }
        return list;
    }

    /**
     * Test API connection
     */
    public boolean testConnection() {
        try {
            List<String> testSource = List.of("Hello");
            List<String> testTarget = List.of("Hei");
            List<AlignmentPair> testPairs = List.of(
                new AlignmentPair(List.of(0), List.of(0), 0.5, "test")
            );

            improveAlignment(testSource, testTarget, testPairs);
            return true;
        } catch (Exception e) {
            logger.log(Level.ERROR, "API connection test failed", e);
            return false;
        }
    }
}
