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
import java.util.Arrays;
import java.util.List;

/**
 * Gale-Church alignment algorithm implementation
 * Based on: "A Program for Aligning Sentences in Bilingual Corpora" (1993)
 *
 * Uses sentence length-based statistical alignment with dynamic programming.
 */
public class GaleChurch {

    // Expected ratio of character lengths (source/target)
    private static final double MEAN_RATIO = 1.0;
    private static final double VARIANCE = 6.8;

    // Match penalties for different alignment types
    private static final double PENALTY_1_1 = 0.0;    // 1:1 match (preferred)
    private static final double PENALTY_1_2 = 230.0;  // 1:2 match
    private static final double PENALTY_2_1 = 230.0;  // 2:1 match
    private static final double PENALTY_2_2 = 440.0;  // 2:2 match
    private static final double PENALTY_0_1 = 450.0;  // Deletion
    private static final double PENALTY_1_0 = 450.0;  // Insertion

    /**
     * Align source and target segments using Gale-Church algorithm
     */
    public List<AlignmentPair> align(List<String> sourceSegments, List<String> targetSegments) {
        int srcLen = sourceSegments.size();
        int tgtLen = targetSegments.size();

        if (srcLen == 0 || tgtLen == 0) {
            return new ArrayList<>();
        }

        // Dynamic programming table
        double[][] cost = new double[srcLen + 1][tgtLen + 1];
        int[][] backtrack = new int[srcLen + 1][tgtLen + 1];

        // Initialize
        for (int i = 0; i <= srcLen; i++) {
            for (int j = 0; j <= tgtLen; j++) {
                cost[i][j] = Double.POSITIVE_INFINITY;
            }
        }
        cost[0][0] = 0;

        // Fill DP table
        for (int i = 0; i <= srcLen; i++) {
            for (int j = 0; j <= tgtLen; j++) {
                if (cost[i][j] == Double.POSITIVE_INFINITY) continue;

                // Try different alignment types
                tryAlignment(cost, backtrack, sourceSegments, targetSegments, i, j, 1, 1, 0); // 1:1
                tryAlignment(cost, backtrack, sourceSegments, targetSegments, i, j, 1, 2, 1); // 1:2
                tryAlignment(cost, backtrack, sourceSegments, targetSegments, i, j, 2, 1, 2); // 2:1
                tryAlignment(cost, backtrack, sourceSegments, targetSegments, i, j, 2, 2, 3); // 2:2
                tryAlignment(cost, backtrack, sourceSegments, targetSegments, i, j, 1, 0, 4); // 1:0 (del)
                tryAlignment(cost, backtrack, sourceSegments, targetSegments, i, j, 0, 1, 5); // 0:1 (ins)
            }
        }

        // Backtrack to find best alignment
        return backtrack(backtrack, sourceSegments, targetSegments, srcLen, tgtLen);
    }

    private void tryAlignment(double[][] cost, int[][] backtrack,
                              List<String> source, List<String> target,
                              int i, int j, int srcStep, int tgtStep, int alignType) {
        int newI = i + srcStep;
        int newJ = j + tgtStep;

        if (newI > source.size() || newJ > target.size()) return;

        // Calculate lengths
        int srcLen = 0;
        for (int k = i; k < newI; k++) {
            if (k < source.size()) srcLen += source.get(k).length();
        }

        int tgtLen = 0;
        for (int k = j; k < newJ; k++) {
            if (k < target.size()) tgtLen += target.get(k).length();
        }

        // Calculate cost using Gale-Church formula
        double matchCost = calculateMatchCost(srcLen, tgtLen, srcStep, tgtStep);
        double newCost = cost[i][j] + matchCost;

        if (newCost < cost[newI][newJ]) {
            cost[newI][newJ] = newCost;
            backtrack[newI][newJ] = alignType;
        }
    }

    private double calculateMatchCost(int srcLen, int tgtLen, int srcCount, int tgtCount) {
        if (srcCount == 0 || tgtCount == 0) {
            return srcCount == 1 ? PENALTY_1_0 : PENALTY_0_1;
        }

        // Avoid division by zero
        if (srcLen == 0) srcLen = 1;

        // Calculate expected target length based on source
        double expectedLen = srcLen * MEAN_RATIO;
        double delta = tgtLen - expectedLen;

        // Gaussian penalty based on length difference
        double lengthPenalty = (delta * delta) / (2 * VARIANCE * srcLen);

        // Type penalty
        double typePenalty = 0;
        if (srcCount == 1 && tgtCount == 1) typePenalty = PENALTY_1_1;
        else if (srcCount == 1 && tgtCount == 2) typePenalty = PENALTY_1_2;
        else if (srcCount == 2 && tgtCount == 1) typePenalty = PENALTY_2_1;
        else if (srcCount == 2 && tgtCount == 2) typePenalty = PENALTY_2_2;

        return lengthPenalty + typePenalty;
    }

    private List<AlignmentPair> backtrack(int[][] backtrack,
                                          List<String> source, List<String> target,
                                          int i, int j) {
        List<AlignmentPair> alignments = new ArrayList<>();

        while (i > 0 || j > 0) {
            int alignType = backtrack[i][j];
            List<Integer> srcIndices = new ArrayList<>();
            List<Integer> tgtIndices = new ArrayList<>();

            switch (alignType) {
                case 0: // 1:1
                    srcIndices.add(i - 1);
                    tgtIndices.add(j - 1);
                    i--; j--;
                    break;
                case 1: // 1:2
                    srcIndices.add(i - 1);
                    tgtIndices.add(j - 2);
                    tgtIndices.add(j - 1);
                    i--; j -= 2;
                    break;
                case 2: // 2:1
                    srcIndices.add(i - 2);
                    srcIndices.add(i - 1);
                    tgtIndices.add(j - 1);
                    i -= 2; j--;
                    break;
                case 3: // 2:2
                    srcIndices.add(i - 2);
                    srcIndices.add(i - 1);
                    tgtIndices.add(j - 2);
                    tgtIndices.add(j - 1);
                    i -= 2; j -= 2;
                    break;
                case 4: // 1:0 (deletion)
                    srcIndices.add(i - 1);
                    i--;
                    break;
                case 5: // 0:1 (insertion)
                    tgtIndices.add(j - 1);
                    j--;
                    break;
            }

            if (!srcIndices.isEmpty() || !tgtIndices.isEmpty()) {
                double confidence = calculateConfidence(alignType, srcIndices, tgtIndices, source, target);
                AlignmentPair pair = new AlignmentPair(srcIndices, tgtIndices,
                    confidence, getAlignmentTypeDescription(alignType));
                alignments.add(0, pair); // Add at beginning (reversing order)
            }
        }

        return alignments;
    }

    private double calculateConfidence(int alignType, List<Integer> srcIndices,
                                       List<Integer> tgtIndices,
                                       List<String> source, List<String> target) {
        double baseConfidence;
        switch (alignType) {
            case 0: baseConfidence = 0.95; break; // 1:1 - high confidence
            case 1: baseConfidence = 0.75; break; // 1:2 - medium confidence
            case 2: baseConfidence = 0.75; break; // 2:1 - medium confidence
            case 3: baseConfidence = 0.60; break; // 2:2 - low confidence
            case 4: baseConfidence = 0.50; break; // 1:0 - very low confidence
            case 5: baseConfidence = 0.50; break; // 0:1 - very low confidence
            default: baseConfidence = 0.50; break;
        }

        // Adjust confidence based on length similarity
        if (!srcIndices.isEmpty() && !tgtIndices.isEmpty()) {
            int srcLen = 0;
            for (int idx : srcIndices) {
                if (idx < source.size()) srcLen += source.get(idx).length();
            }

            int tgtLen = 0;
            for (int idx : tgtIndices) {
                if (idx < target.size()) tgtLen += target.get(idx).length();
            }

            double ratio = (double) Math.max(srcLen, tgtLen) / Math.max(1, Math.min(srcLen, tgtLen));
            if (ratio > 3.0) {
                baseConfidence *= 0.8; // Penalize very different lengths
            } else if (ratio > 2.0) {
                baseConfidence *= 0.9;
            }
        }

        return Math.max(0.0, Math.min(1.0, baseConfidence));
    }

    private String getAlignmentTypeDescription(int alignType) {
        switch (alignType) {
            case 0: return "1:1 match";
            case 1: return "1:2 match (source split in target)";
            case 2: return "2:1 match (source merged in target)";
            case 3: return "2:2 match (complex)";
            case 4: return "Deletion (no target)";
            case 5: return "Insertion (no source)";
            default: return "Unknown";
        }
    }
}
