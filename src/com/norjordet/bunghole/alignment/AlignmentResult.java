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

package com.norjordet.bunghole.alignment;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Result of an alignment operation, containing aligned pairs and statistics
 */
public class AlignmentResult {
    private List<AlignmentPair> pairs;
    private double overallConfidence;
    private List<AlignmentPair> uncertainPairs;
    private int aiReviewedCount;

    public AlignmentResult(List<AlignmentPair> pairs,
                          double overallConfidence,
                          List<AlignmentPair> uncertainPairs) {
        this.pairs = new ArrayList<>(pairs);
        this.overallConfidence = overallConfidence;
        this.uncertainPairs = new ArrayList<>(uncertainPairs);
        this.aiReviewedCount = 0;
    }

    // Getters
    public List<AlignmentPair> getPairs() {
        return new ArrayList<>(pairs);
    }

    public List<AlignmentPair> getAllPairs() {
        return new ArrayList<>(pairs);
    }

    public double getOverallConfidence() {
        return overallConfidence;
    }

    public List<AlignmentPair> getUncertainPairs() {
        return new ArrayList<>(uncertainPairs);
    }

    public int getUncertainCount() {
        return uncertainPairs.size();
    }

    public boolean needsAIReview() {
        return uncertainPairs.size() > 0;
    }

    public int getAiReviewedCount() {
        return aiReviewedCount;
    }

    public void setAiReviewedCount(int count) {
        this.aiReviewedCount = count;
    }

    // Statistics
    public int getTotalPairs() {
        return pairs.size();
    }

    public int getConfidentPairs() {
        return (int) pairs.stream()
            .filter(p -> !p.isUncertain())
            .count();
    }

    public double getConfidentPercent() {
        if (pairs.isEmpty()) return 0.0;
        return (getConfidentPairs() * 100.0) / pairs.size();
    }

    public int getOneToOneCount() {
        return (int) pairs.stream()
            .filter(AlignmentPair::isOneToOne)
            .count();
    }

    public int getComplexCount() {
        return (int) pairs.stream()
            .filter(p -> !p.isOneToOne())
            .count();
    }

    /**
     * Update result with AI-improved pairs
     */
    public void updateWithAIPairs(List<AlignmentPair> aiImprovedPairs) {
        for (AlignmentPair aiPair : aiImprovedPairs) {
            // Find and replace matching pair
            for (int i = 0; i < pairs.size(); i++) {
                AlignmentPair existingPair = pairs.get(i);
                if (existingPair.getSourceIndices().equals(aiPair.getSourceIndices())) {
                    pairs.set(i, aiPair);
                    aiReviewedCount++;
                    break;
                }
            }
        }

        // Recalculate uncertain pairs
        uncertainPairs = pairs.stream()
            .filter(AlignmentPair::isUncertain)
            .collect(Collectors.toList());

        // Recalculate overall confidence
        overallConfidence = pairs.stream()
            .mapToDouble(AlignmentPair::getConfidence)
            .average()
            .orElse(0.0);
    }

    /**
     * Convert to JSON for API response
     */
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        json.put("totalPairs", getTotalPairs());
        json.put("confidentPairs", getConfidentPairs());
        json.put("uncertainPairs", getUncertainCount());
        json.put("oneToOneCount", getOneToOneCount());
        json.put("complexCount", getComplexCount());
        json.put("overallConfidence", overallConfidence);
        json.put("confidencePercent", getConfidentPercent());
        json.put("aiReviewedCount", aiReviewedCount);
        json.put("needsAIReview", needsAIReview());

        // Add pairs
        JSONArray pairsArray = new JSONArray();
        for (AlignmentPair pair : pairs) {
            JSONObject pairJson = new JSONObject();
            pairJson.put("source", pair.getSourceIndices());
            pairJson.put("target", pair.getTargetIndices());
            pairJson.put("confidence", pair.getConfidence());
            pairJson.put("note", pair.getNote());
            pairJson.put("aiReviewed", pair.isAiReviewed());
            pairJson.put("type", pair.getAlignmentType());
            pairsArray.put(pairJson);
        }
        json.put("pairs", pairsArray);

        return json;
    }

    @Override
    public String toString() {
        return String.format(
            "AlignmentResult[pairs=%d, confident=%.1f%%, uncertain=%d, aiReviewed=%d]",
            getTotalPairs(), getConfidentPercent(), getUncertainCount(), aiReviewedCount
        );
    }
}
