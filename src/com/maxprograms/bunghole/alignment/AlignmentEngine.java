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

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.maxprograms.xml.Element;
import com.maxprograms.xml.TextNode;
import com.maxprograms.xml.XMLNode;

/**
 * Main alignment engine that coordinates Hunalign/Gale-Church algorithms
 */
public class AlignmentEngine {

    private static final Logger logger = System.getLogger(AlignmentEngine.class.getName());
    private GaleChurch galeChurch;
    private HunalignService hunalign;
    private boolean useHunalign;

    public AlignmentEngine(String appPath) {
        this.galeChurch = new GaleChurch();

        // Initialize Hunalign if available
        String hunalignPath = appPath + "/bin/hunalign/hunalign";
        String dictionaryPath = appPath + "/dictionaries/en-no.dic";
        this.hunalign = new HunalignService(hunalignPath, dictionaryPath);
        this.useHunalign = hunalign.isAvailable();

        if (useHunalign) {
            logger.log(Level.INFO, "Hunalign is available and will be used for alignment");
        } else {
            logger.log(Level.INFO, "Hunalign not available, falling back to Gale-Church");
        }
    }

    /**
     * Perform alignment on source and target XML elements
     * Uses two-pass hybrid strategy for optimal quality
     */
    public AlignmentResult performAlignment(List<Element> sources, List<Element> targets) {
        // Convert elements to strings
        List<String> sourceStrings = sources.stream()
            .map(this::extractText)
            .collect(Collectors.toList());

        List<String> targetStrings = targets.stream()
            .map(this::extractText)
            .collect(Collectors.toList());

        // PASS 1: Run Gale-Church on all segments (fast baseline)
        logger.log(Level.INFO, "Pass 1: Running Gale-Church alignment on all segments...");
        List<AlignmentPair> galeChurchPairs = galeChurch.align(sourceStrings, targetStrings);

        // If Hunalign not available, return Gale-Church results
        if (!useHunalign) {
            logger.log(Level.INFO, "Hunalign not available, using Gale-Church results only");
            return createResult(galeChurchPairs, "Gale-Church");
        }

        // PASS 2: Run Hunalign on uncertain segments to improve quality
        logger.log(Level.INFO, "Pass 2: Running Hunalign on uncertain segments...");
        List<AlignmentPair> finalPairs = new ArrayList<>();
        int improvedCount = 0;
        int agreedCount = 0;

        try {
            // Run Hunalign on all segments for comparison
            List<AlignmentPair> hunalignPairs = hunalign.align(sourceStrings, targetStrings);

            // Compare results segment by segment
            for (int i = 0; i < galeChurchPairs.size() && i < hunalignPairs.size(); i++) {
                AlignmentPair gcPair = galeChurchPairs.get(i);
                AlignmentPair haPair = hunalignPairs.get(i);

                // Check if both algorithms agree
                if (pairsMatch(gcPair, haPair)) {
                    agreedCount++;
                    // Both agree - boost confidence and note agreement
                    AlignmentPair boostedPair = new AlignmentPair(
                        gcPair.getSourceIndices(),
                        gcPair.getTargetIndices(),
                        Math.min(0.95, gcPair.getConfidence() + 0.15), // Boost confidence
                        "Agreement: Gale-Church + Hunalign"
                    );
                    finalPairs.add(boostedPair);
                } else if (gcPair.isUncertain()) {
                    // Gale-Church uncertain - prefer Hunalign's suggestion
                    improvedCount++;
                    AlignmentPair improvedPair = new AlignmentPair(
                        haPair.getSourceIndices(),
                        haPair.getTargetIndices(),
                        haPair.getConfidence(),
                        "Hunalign (improved uncertain Gale-Church)"
                    );
                    finalPairs.add(improvedPair);
                } else {
                    // Gale-Church confident but Hunalign disagrees - flag for review
                    AlignmentPair flaggedPair = new AlignmentPair(
                        gcPair.getSourceIndices(),
                        gcPair.getTargetIndices(),
                        Math.max(0.65, gcPair.getConfidence() - 0.1), // Reduce confidence slightly
                        "Gale-Church (algorithms disagree)"
                    );
                    finalPairs.add(flaggedPair);
                }
            }

            logger.log(Level.INFO, "Hybrid alignment complete: {0} agreements, {1} improvements",
                new Object[]{agreedCount, improvedCount});

        } catch (IOException e) {
            logger.log(Level.WARNING, "Hunalign failed, using Gale-Church results: " + e.getMessage());
            return createResult(galeChurchPairs, "Gale-Church (Hunalign failed)");
        }

        return createResult(finalPairs, "Hybrid (Gale-Church + Hunalign)");
    }

    /**
     * Check if two alignment pairs match (same source/target indices)
     */
    private boolean pairsMatch(AlignmentPair p1, AlignmentPair p2) {
        return p1.getSourceIndices().equals(p2.getSourceIndices()) &&
               p1.getTargetIndices().equals(p2.getTargetIndices());
    }

    /**
     * Create AlignmentResult with statistics
     */
    private AlignmentResult createResult(List<AlignmentPair> pairs, String method) {
        double avgConfidence = pairs.stream()
            .mapToDouble(AlignmentPair::getConfidence)
            .average()
            .orElse(0.0);

        List<AlignmentPair> uncertainPairs = pairs.stream()
            .filter(AlignmentPair::isUncertain)
            .collect(Collectors.toList());

        logger.log(Level.INFO, "Alignment method: {0}, Avg confidence: {1}, Uncertain: {2}/{3}",
            new Object[]{method, String.format("%.2f", avgConfidence), uncertainPairs.size(), pairs.size()});

        return new AlignmentResult(pairs, avgConfidence, uncertainPairs);
    }

    /**
     * Extract pure text from XML element (including nested elements)
     */
    private String extractText(Element element) {
        StringBuilder text = new StringBuilder();
        extractTextRecursive(element, text);
        return text.toString().trim();
    }

    private void extractTextRecursive(Element element, StringBuilder text) {
        List<XMLNode> nodes = element.getContent();
        for (XMLNode node : nodes) {
            if (node.getNodeType() == XMLNode.TEXT_NODE) {
                text.append(((TextNode) node).getText());
            } else if (node.getNodeType() == XMLNode.ELEMENT_NODE) {
                Element e = (Element) node;
                if ("g".equals(e.getName())) {
                    // Recursively extract text from group elements
                    extractTextRecursive(e, text);
                }
                // ph elements are ignored in pure text extraction
            }
        }
    }

    /**
     * Get text strings from elements for AI processing
     */
    public List<String> getTextStrings(List<Element> elements) {
        return elements.stream()
            .map(this::extractText)
            .collect(Collectors.toList());
    }
}
