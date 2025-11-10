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

package com.norjordet.bunghole;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * Security utilities for input validation and sanitization
 */
public class SecurityUtils {

    private SecurityUtils() {
        // Utility class - prevent instantiation
    }

    // Allowed file extensions for alignment files
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        ".algn", ".tmx", ".csv", ".xlsx", ".txt", ".html", ".xml"
    );

    /**
     * Validates a file path to prevent path traversal attacks and ensure
     * the file is within allowed locations
     *
     * @param filePath the file path to validate
     * @return the canonical path if valid
     * @throws SecurityException if the path is invalid or unsafe
     */
    public static String validateFilePath(String filePath) throws SecurityException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new SecurityException("File path cannot be null or empty");
        }

        try {
            Path path = Paths.get(filePath);
            File file = path.toFile();

            // Get canonical path to resolve any .. or symbolic links
            String canonicalPath = file.getCanonicalPath();

            // Check for path traversal attempts
            if (canonicalPath.contains("..")) {
                throw new SecurityException("Path traversal detected: " + filePath);
            }

            // Validate file extension if file exists
            if (file.exists() && file.isFile()) {
                String fileName = file.getName().toLowerCase();
                boolean hasAllowedExtension = ALLOWED_EXTENSIONS.stream()
                    .anyMatch(fileName::endsWith);

                if (!hasAllowedExtension) {
                    throw new SecurityException("File extension not allowed: " + fileName);
                }
            }

            return canonicalPath;

        } catch (InvalidPathException e) {
            throw new SecurityException("Invalid file path: " + filePath, e);
        } catch (IOException e) {
            throw new SecurityException("Error validating file path: " + filePath, e);
        }
    }

    /**
     * Validates that a file exists and is readable
     *
     * @param filePath the file path to check
     * @throws SecurityException if the file doesn't exist or isn't readable
     */
    public static void validateFileExists(String filePath) throws SecurityException {
        String validPath = validateFilePath(filePath);
        Path path = Paths.get(validPath);

        if (!Files.exists(path)) {
            throw new SecurityException("File does not exist: " + filePath);
        }

        if (!Files.isReadable(path)) {
            throw new SecurityException("File is not readable: " + filePath);
        }

        if (Files.isDirectory(path)) {
            throw new SecurityException("Path is a directory, not a file: " + filePath);
        }
    }

    /**
     * Validates that a directory exists and is writable
     *
     * @param dirPath the directory path to check
     * @throws SecurityException if the directory doesn't exist or isn't writable
     */
    public static void validateDirectory(String dirPath) throws SecurityException {
        String validPath = validateFilePath(dirPath);
        Path path = Paths.get(validPath);

        if (!Files.exists(path)) {
            throw new SecurityException("Directory does not exist: " + dirPath);
        }

        if (!Files.isDirectory(path)) {
            throw new SecurityException("Path is not a directory: " + dirPath);
        }

        if (!Files.isWritable(path)) {
            throw new SecurityException("Directory is not writable: " + dirPath);
        }
    }

    /**
     * Sanitizes a JSON string value by removing potentially dangerous characters
     *
     * @param value the string to sanitize
     * @return the sanitized string
     */
    public static String sanitizeString(String value) {
        if (value == null) {
            return "";
        }

        // Remove control characters except newline and tab
        return value.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "")
                   .trim();
    }

    /**
     * Validates that a string is not empty after sanitization
     *
     * @param value the string to validate
     * @param fieldName the name of the field for error messages
     * @throws SecurityException if the string is empty
     */
    public static String validateNonEmpty(String value, String fieldName) throws SecurityException {
        String sanitized = sanitizeString(value);
        if (sanitized.isEmpty()) {
            throw new SecurityException(fieldName + " cannot be empty");
        }
        return sanitized;
    }

    /**
     * Validates an integer is within acceptable range
     *
     * @param value the value to validate
     * @param min minimum acceptable value
     * @param max maximum acceptable value
     * @param fieldName the name of the field for error messages
     * @throws SecurityException if the value is out of range
     */
    public static int validateIntRange(int value, int min, int max, String fieldName) throws SecurityException {
        if (value < min || value > max) {
            throw new SecurityException(fieldName + " must be between " + min + " and " + max);
        }
        return value;
    }
}
