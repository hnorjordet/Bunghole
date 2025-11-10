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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for running Hunalign sentence aligner
 */
public class HunalignService {

    private static final Logger logger = System.getLogger(HunalignService.class.getName());
    private String hunalignPath;
    private String dictionaryPath;

    public HunalignService(String hunalignPath, String dictionaryPath) {
        this.hunalignPath = hunalignPath;
        this.dictionaryPath = dictionaryPath;
    }

    /**
     * Run Hunalign and return alignment pairs
     */
    public List<AlignmentPair> align(List<String> sourceSegments, List<String> targetSegments)
            throws IOException {
        // Create temp files for source and target
        File sourceFile = createTempTextFile(sourceSegments);
        File targetFile = createTempTextFile(targetSegments);

        try {
            // Run hunalign
            List<AlignmentPair> pairs = runHunalign(sourceFile, targetFile);
            return pairs;
        } finally {
            // Cleanup temp files
            sourceFile.delete();
            targetFile.delete();
        }
    }

    /**
     * Create temporary text file with one segment per line
     */
    private File createTempTextFile(List<String> segments) throws IOException {
        File tempFile = File.createTempFile("hunalign", ".txt");
        tempFile.deleteOnExit();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            for (String segment : segments) {
                // Clean text: remove line breaks, trim
                String cleaned = segment.replace("\n", " ").replace("\r", " ").trim();
                writer.write(cleaned);
                writer.newLine();
            }
        }

        return tempFile;
    }

    /**
     * Run hunalign process and parse output
     */
    private List<AlignmentPair> runHunalign(File sourceFile, File targetFile) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
            hunalignPath,
            "-text",           // Text output format
            "-utf",            // UTF-8 encoding
            "-realign",        // Use dictionary for better alignment
            dictionaryPath,
            sourceFile.getAbsolutePath(),
            targetFile.getAbsolutePath()
        );

        logger.log(Level.INFO, "Running Hunalign: " + String.join(" ", pb.command()));

        Process process = pb.start();

        // Read output
        List<String> outputLines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                outputLines.add(line);
            }
        }

        // Wait for completion
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                // Read error output
                StringBuilder error = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        error.append(line).append("\n");
                    }
                }
                logger.log(Level.WARNING, "Hunalign exit code: " + exitCode);
                logger.log(Level.WARNING, "Hunalign error: " + error.toString());
            }
        } catch (InterruptedException e) {
            throw new IOException("Hunalign process interrupted", e);
        }

        // Parse output
        return parseHunalignOutput(outputLines);
    }

    /**
     * Parse Hunalign text output format
     * Format: source_line_nums TAB target_line_nums TAB confidence
     * Example: "1	1	0.95" (1:1 mapping)
     * Example: "2-3	2	0.72" (2:1 mapping)
     */
    private List<AlignmentPair> parseHunalignOutput(List<String> lines) {
        List<AlignmentPair> pairs = new ArrayList<>();

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }

            String[] parts = line.split("\t");
            if (parts.length < 2) {
                logger.log(Level.WARNING, "Invalid Hunalign output line: " + line);
                continue;
            }

            try {
                // Parse source indices (e.g., "1" or "2-3")
                List<Integer> sourceIndices = parseIndices(parts[0]);

                // Parse target indices
                List<Integer> targetIndices = parseIndices(parts[1]);

                // Parse confidence (if available)
                // Default to 0.6 (medium) if Hunalign doesn't provide confidence score
                double confidence = parts.length >= 3 ? Double.parseDouble(parts[2]) : 0.6;

                // Determine note based on alignment type
                String note;
                if (sourceIndices.size() == 1 && targetIndices.size() == 1) {
                    note = "1:1 (Hunalign)";
                } else if (sourceIndices.size() == 2 && targetIndices.size() == 1) {
                    note = "2:1 (Hunalign - NEEDS REVIEW)";
                } else if (sourceIndices.size() == 1 && targetIndices.size() == 2) {
                    note = "1:2 (Hunalign - NEEDS REVIEW)";
                } else {
                    note = String.format("%d:%d (Hunalign - NEEDS REVIEW)",
                        sourceIndices.size(), targetIndices.size());
                }

                // Create alignment pair with note
                AlignmentPair pair = new AlignmentPair(sourceIndices, targetIndices, confidence, note);

                // Mark multi-mappings as AI-reviewed (needs manual review)
                if (!pair.isOneToOne()) {
                    pair.setAiReviewed(false); // Will be sent to Claude
                    // Lower confidence for multi-mappings to ensure AI review
                    pair.setConfidence(Math.min(confidence, 0.70));
                }

                pairs.add(pair);

            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "Failed to parse line: " + line, e);
            }
        }

        logger.log(Level.INFO, String.format("Hunalign produced %d alignment pairs", pairs.size()));
        return pairs;
    }

    /**
     * Parse index string like "1" or "2-3" or "1-2-3"
     * Hunalign uses 1-based indexing, we convert to 0-based
     */
    private List<Integer> parseIndices(String indexStr) {
        List<Integer> indices = new ArrayList<>();

        if (indexStr.contains("-")) {
            String[] parts = indexStr.split("-");
            for (String part : parts) {
                int index = Integer.parseInt(part.trim()) - 1; // Convert to 0-based
                indices.add(index);
            }
        } else {
            int index = Integer.parseInt(indexStr.trim()) - 1; // Convert to 0-based
            indices.add(index);
        }

        return indices;
    }

    /**
     * Check if Hunalign is available
     */
    public boolean isAvailable() {
        try {
            logger.log(Level.INFO, "Checking Hunalign availability at: {0}", hunalignPath);

            File hunalignFile = new File(hunalignPath);
            if (!hunalignFile.exists()) {
                logger.log(Level.WARNING, "Hunalign binary not found at: {0}", hunalignPath);
                return false;
            }

            if (!hunalignFile.canExecute()) {
                logger.log(Level.WARNING, "Hunalign binary not executable at: {0}", hunalignPath);
                return false;
            }

            ProcessBuilder pb = new ProcessBuilder(hunalignPath, "--help");
            Process process = pb.start();
            int exitCode = process.waitFor();

            // Hunalign returns 255 for --help, which is fine as long as it runs
            if (exitCode == 0 || exitCode == 1 || exitCode == 255) {
                logger.log(Level.INFO, "Hunalign is available and executable (exit code: {0})", exitCode);
                return true;
            } else {
                logger.log(Level.WARNING, "Hunalign returned unexpected exit code: {0}", exitCode);
                return false;
            }
        } catch (IOException | InterruptedException e) {
            logger.log(Level.WARNING, "Error checking Hunalign availability: {0}", e.getMessage());
            return false;
        }
    }
}
