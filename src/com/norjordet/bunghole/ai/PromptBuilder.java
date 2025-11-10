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
 * Builds prompts for Claude AI alignment improvement
 */
public class PromptBuilder {

    /**
     * Build a prompt for improving uncertain alignments
     */
    public static String buildAlignmentPrompt(
        List<String> sourceSegments,
        List<String> targetSegments,
        List<AlignmentPair> uncertainPairs
    ) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an expert translation memory alignment specialist.\n\n");
        prompt.append("CRITICAL RULE: You are ONLY matching existing translations, NOT creating new ones!\n");
        prompt.append("The Norwegian translations already exist in the document - your job is to find which one matches which English segment.\n\n");
        prompt.append("TASK: Find the correct alignment between English (source) and Norwegian (target) segments.\n");
        prompt.append("IMPORTANT: The current alignments may be WRONG due to punctuation or sentence splitting differences.\n");
        prompt.append("Your job is to search through ALL target segments to find the BEST EXISTING match for each uncertain source segment.\n\n");
        prompt.append("DO NOT:\n");
        prompt.append("- Translate English to Norwegian\n");
        prompt.append("- Generate new Norwegian text\n");
        prompt.append("- Use your knowledge of Norwegian to create translations\n\n");
        prompt.append("DO:\n");
        prompt.append("- Find the closest semantic match among EXISTING target segments\n");
        prompt.append("- Look for near-exact matches with minor wording differences\n");
        prompt.append("- Check nearby segments (±1, ±2, ±3 positions) for shifted content\n");
        prompt.append("- Consider that professional translators may use domain-specific terminology\n\n");

        // Add source segments
        prompt.append("SOURCE SEGMENTS (English):\n");
        for (int i = 0; i < sourceSegments.size(); i++) {
            prompt.append(String.format("S%d: %s\n", i, escapeText(sourceSegments.get(i))));
        }
        prompt.append("\n");

        // Add target segments
        prompt.append("TARGET SEGMENTS (Norwegian):\n");
        for (int i = 0; i < targetSegments.size(); i++) {
            prompt.append(String.format("T%d: %s\n", i, escapeText(targetSegments.get(i))));
        }
        prompt.append("\n");

        // Add uncertain alignments
        prompt.append("UNCERTAIN ALIGNMENTS TO REVIEW:\n");
        for (AlignmentPair pair : uncertainPairs) {
            prompt.append(String.format("- S%s <-> T%s (Confidence: %.2f, Reason: %s)\n",
                listToString(pair.getSourceIndices()),
                listToString(pair.getTargetIndices()),
                pair.getConfidence(),
                pair.getNote()
            ));
        }
        prompt.append("\n");

        // Instructions
        prompt.append("INSTRUCTIONS:\n");
        prompt.append("1. For EACH uncertain alignment:\n");
        prompt.append("   a) Look at the proposed target segment(s)\n");
        prompt.append("   b) SEARCH EXHAUSTIVELY through ALL target segments (T0, T1, T2, ...) for the BEST EXISTING match\n");
        prompt.append("   c) Matching strategy:\n");
        prompt.append("      - First: Look for EXACT or NEAR-EXACT semantic matches (different words, same meaning)\n");
        prompt.append("      - Second: Check segments within ±5 positions for shifted content\n");
        prompt.append("      - Third: Look for domain-specific terminology (medical, technical, legal terms)\n");
        prompt.append("      - Example: 'Pharmacodynamic properties' → prefer '<Farmakodynamiske effekter>' over inventing 'Farmakodynamiske egenskaper'\n");
        prompt.append("   d) Pay special attention to:\n");
        prompt.append("      - Punctuation differences (., :, ; etc.) that may have caused wrong splits\n");
        prompt.append("      - Sentence boundaries that differ between languages\n");
        prompt.append("      - Content that appears slightly shifted (e.g., T5 might be better than T4)\n");
        prompt.append("\n");
        prompt.append("2. Common misalignment causes:\n");
        prompt.append("   - English splits at \":\" but Norwegian keeps it as one sentence\n");
        prompt.append("   - Period \".\" placement differs, causing off-by-one errors\n");
        prompt.append("   - Commas \",\" or semicolons \";\" split differently\n");
        prompt.append("   - Headers or lists formatted differently\n");
        prompt.append("\n");
        prompt.append("3. How to fix:\n");
        prompt.append("   - If you find a better target segment, use that index instead\n");
        prompt.append("   - You can propose ANY target index (T0 to T" + (targetSegments.size()-1) + ")\n");
        prompt.append("   - Consider 1:1, 1:2, 2:1, or 2:2 mappings if content was merged/split\n");
        prompt.append("   - Set confidence >0.85 if you're sure, 0.70-0.85 if probable, <0.70 if uncertain\n");
        prompt.append("\n");
        prompt.append("4. In your 'note' field, explain:\n");
        prompt.append("   - If you kept the original alignment: why it's correct\n");
        prompt.append("   - If you changed it: which target you chose and why (e.g., 'T5 is better match, punctuation caused off-by-one')\n\n");

        // Response format
        prompt.append("RESPONSE FORMAT (JSON only, no other text):\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"alignments\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"source\": [0],\n");
        prompt.append("      \"target\": [0],\n");
        prompt.append("      \"confidence\": 0.95,\n");
        prompt.append("      \"note\": \"Kept original - correct match\"\n");
        prompt.append("    },\n");
        prompt.append("    {\n");
        prompt.append("      \"source\": [1],\n");
        prompt.append("      \"target\": [2],\n");
        prompt.append("      \"confidence\": 0.88,\n");
        prompt.append("      \"note\": \"Changed from T1 to T2 - punctuation caused off-by-one\"\n");
        prompt.append("    },\n");
        prompt.append("    {\n");
        prompt.append("      \"source\": [3, 4],\n");
        prompt.append("      \"target\": [5],\n");
        prompt.append("      \"confidence\": 0.90,\n");
        prompt.append("      \"note\": \"Two English sentences merged into one Norwegian sentence at T5\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("```\n");
        prompt.append("\n");
        prompt.append("IMPORTANT: You can and SHOULD change target indices if you find better EXISTING matches!\n");
        prompt.append("Don't just confirm the current alignment - actively search for improvements.\n\n");
        prompt.append("FINAL REMINDER:\n");
        prompt.append("- Your response must ONLY contain target indices that EXIST in the TARGET SEGMENTS list above\n");
        prompt.append("- You are matching existing professional translations, not creating new ones\n");
        prompt.append("- If a target segment uses unexpected terminology (e.g., 'effekter' instead of 'egenskaper'), that's the correct professional choice - use it!\n");
        prompt.append("- Trust the translator's domain expertise - your job is to FIND their translation, not replace it\n");

        return prompt.toString();
    }

    /**
     * Escape special characters in text
     */
    private static String escapeText(String text) {
        if (text == null) return "";
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", " ")
            .replace("\r", "");
    }

    /**
     * Convert list of integers to string representation
     */
    private static String listToString(List<Integer> list) {
        if (list.isEmpty()) return "[]";
        if (list.size() == 1) return String.valueOf(list.get(0));
        return list.toString();
    }

    /**
     * Build a simple test prompt
     */
    public static String buildTestPrompt() {
        return "You are a translation alignment expert. " +
               "Respond with: {\"status\": \"ok\"}";
    }
}
