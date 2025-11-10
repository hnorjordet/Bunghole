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

/**
 * Cost estimate for AI processing
 */
public class CostEstimate {
    private final int estimatedInputTokens;
    private final int estimatedOutputTokens;
    private final double estimatedCost;
    private final String providerName;
    private final String modelName;

    public CostEstimate(
        int estimatedInputTokens,
        int estimatedOutputTokens,
        double estimatedCost,
        String providerName,
        String modelName
    ) {
        this.estimatedInputTokens = estimatedInputTokens;
        this.estimatedOutputTokens = estimatedOutputTokens;
        this.estimatedCost = estimatedCost;
        this.providerName = providerName;
        this.modelName = modelName;
    }

    public int getEstimatedInputTokens() {
        return estimatedInputTokens;
    }

    public int getEstimatedOutputTokens() {
        return estimatedOutputTokens;
    }

    public double getEstimatedCost() {
        return estimatedCost;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getModelName() {
        return modelName;
    }
}
