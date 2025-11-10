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

import java.util.List;
import com.norjordet.bunghole.alignment.AlignmentPair;

/**
 * Utility for estimating Claude API costs
 */
public class CostEstimator {

    // Claude Sonnet 3.5 pricing (per million tokens)
    private static final double INPUT_COST_PER_MILLION = 3.0;
    private static final double OUTPUT_COST_PER_MILLION = 15.0;

    // Average tokens per character (rough estimate)
    private static final double TOKENS_PER_CHAR = 0.25;

    // Average expected output size
    private static final int AVG_OUTPUT_TOKENS_PER_PAIR = 50;

    /**
     * Estimate cost for improving alignments
     */
    public static CostEstimate estimateCost(
        List<String> sourceSegments,
        List<String> targetSegments,
        List<AlignmentPair> uncertainPairs
    ) {
        // Calculate prompt size
        int promptChars = 0;

        // System prompt and instructions (fixed overhead)
        promptChars += 1000;

        // Source segments
        for (String seg : sourceSegments) {
            promptChars += seg.length() + 10; // +10 for formatting
        }

        // Target segments
        for (String seg : targetSegments) {
            promptChars += seg.length() + 10;
        }

        // Uncertain pairs description
        promptChars += uncertainPairs.size() * 50;

        // Convert to tokens
        int inputTokens = (int) (promptChars * TOKENS_PER_CHAR);
        int outputTokens = uncertainPairs.size() * AVG_OUTPUT_TOKENS_PER_PAIR;

        // Calculate cost
        double inputCost = (inputTokens / 1_000_000.0) * INPUT_COST_PER_MILLION;
        double outputCost = (outputTokens / 1_000_000.0) * OUTPUT_COST_PER_MILLION;
        double totalCost = inputCost + outputCost;

        return new CostEstimate(
            inputTokens,
            outputTokens,
            inputCost,
            outputCost,
            totalCost,
            uncertainPairs.size()
        );
    }

    /**
     * Cost estimate result
     */
    public static class CostEstimate {
        private int inputTokens;
        private int outputTokens;
        private double inputCost;
        private double outputCost;
        private double totalCost;
        private int pairsToReview;

        public CostEstimate(int inputTokens, int outputTokens,
                           double inputCost, double outputCost,
                           double totalCost, int pairsToReview) {
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
            this.inputCost = inputCost;
            this.outputCost = outputCost;
            this.totalCost = totalCost;
            this.pairsToReview = pairsToReview;
        }

        public int getInputTokens() { return inputTokens; }
        public int getOutputTokens() { return outputTokens; }
        public int getTotalTokens() { return inputTokens + outputTokens; }
        public double getInputCost() { return inputCost; }
        public double getOutputCost() { return outputCost; }
        public double getTotalCost() { return totalCost; }
        public int getPairsToReview() { return pairsToReview; }

        /**
         * Get formatted cost string for display
         */
        public String getFormattedCost() {
            if (totalCost < 0.01) {
                return String.format("$%.4f", totalCost);
            } else if (totalCost < 1.0) {
                return String.format("$%.3f", totalCost);
            } else {
                return String.format("$%.2f", totalCost);
            }
        }

        /**
         * Get detailed breakdown for UI
         */
        public String getDetailedBreakdown() {
            return String.format(
                "Pairs to review: %d\n" +
                "Input tokens: %,d (~$%.4f)\n" +
                "Output tokens: %,d (~$%.4f)\n" +
                "Total estimated cost: %s",
                pairsToReview,
                inputTokens, inputCost,
                outputTokens, outputCost,
                getFormattedCost()
            );
        }

        @Override
        public String toString() {
            return String.format("CostEstimate[pairs=%d, tokens=%d, cost=%s]",
                pairsToReview, getTotalTokens(), getFormattedCost());
        }
    }
}
