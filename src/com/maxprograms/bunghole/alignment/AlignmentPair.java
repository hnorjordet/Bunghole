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

package com.maxprograms.bunghole.alignment;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents a pair of aligned segments (source <-> target)
 */
public class AlignmentPair {
    private List<Integer> sourceIndices;
    private List<Integer> targetIndices;
    private double confidence;
    private String note;
    private boolean aiReviewed;

    public AlignmentPair(List<Integer> sourceIndices, List<Integer> targetIndices,
                         double confidence, String note) {
        this.sourceIndices = new ArrayList<>(sourceIndices);
        this.targetIndices = new ArrayList<>(targetIndices);
        this.confidence = confidence;
        this.note = note;
        this.aiReviewed = false;
    }

    // Getters
    public List<Integer> getSourceIndices() {
        return new ArrayList<>(sourceIndices);
    }

    public List<Integer> getTargetIndices() {
        return new ArrayList<>(targetIndices);
    }

    public double getConfidence() {
        return confidence;
    }

    public String getNote() {
        return note;
    }

    public boolean isAiReviewed() {
        return aiReviewed;
    }

    // Setters
    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setAiReviewed(boolean aiReviewed) {
        this.aiReviewed = aiReviewed;
    }

    // Helper methods
    public boolean isOneToOne() {
        return sourceIndices.size() == 1 && targetIndices.size() == 1;
    }

    public boolean isUncertain() {
        return confidence < 0.75; // Threshold for AI review
    }

    public String getAlignmentType() {
        return sourceIndices.size() + ":" + targetIndices.size();
    }

    @Override
    public String toString() {
        return String.format("S%s <-> T%s (%.2f) [%s]%s",
            sourceIndices, targetIndices, confidence, note,
            aiReviewed ? " ✓AI" : "");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        AlignmentPair other = (AlignmentPair) obj;
        return sourceIndices.equals(other.sourceIndices) &&
               targetIndices.equals(other.targetIndices);
    }

    @Override
    public int hashCode() {
        return sourceIndices.hashCode() * 31 + targetIndices.hashCode();
    }
}
