package com.rdc.admin.util;

/**
 * Utility class for creating URL-friendly slugs from strings.
 * This class cannot be instantiated and only provides static methods.
 */
public class SlugGenerator {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private SlugGenerator() {
        // Utility classes should not be instantiated.
    }

    /**
     * Converts a string (e.g., a title) into a URL-safe slug.
     * * The process involves:
     * 1. Converting the entire string to lowercase.
     * 2. Replacing all characters that are NOT alphanumeric, space, or hyphen with nothing.
     * 3. Replacing all spaces with hyphens (-).
     * 4. Trimming any leading or trailing hyphens or spaces.
     * * Example: "My Awesome New Design!" -> "my-awesome-new-design"
     *
     * @param text The input string to convert.
     * @return The resulting slug.
     */
    public static String generateSlug(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        return text.toLowerCase()
                // Remove any characters that are not a-z, 0-9, space, or hyphen
                .replaceAll("[^a-z0-9\\s-]", "")
                // Replace one or more spaces with a single hyphen
                .replaceAll("\\s+", "-")
                // Remove multiple consecutive hyphens (e.g., from double spaces)
                .replaceAll("-+", "-")
                // Trim leading/trailing hyphens or spaces
                .trim();
    }
}