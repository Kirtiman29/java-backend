package com.rdc.admin.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility class for generating URL-friendly slugs from strings.
 */
public class SlugUtils {

    // Pattern to replace non-alphanumeric characters (except dashes and underscores)
    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");

    // Pattern to replace multiple consecutive dashes
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    // Pattern to remove starting/ending dashes
    private static final Pattern EDGES = Pattern.compile("^-+|-+$");

    // Pattern to normalize multiple dashes into one
    private static final Pattern MULTIPLE_DASHES = Pattern.compile("-+");

    /**
     * Converts a string into a URL-friendly slug.
     * * @param input the string to slugify (e.g., "Abstract Geometric Design")
     * @return the slugified string (e.g., "abstract-geometric-design")
     */
    public static String toSlug(String input) {
        if (input == null) {
            return "";
        }

        // 1. Convert to lower case
        String nowhitespace = input.toLowerCase(Locale.ENGLISH);

        // 2. Normalize and replace non-Latin characters (e.g., accents)
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        normalized = NON_LATIN.matcher(normalized).replaceAll("");

        // 3. Replace spaces with dashes
        String slug = WHITESPACE.matcher(normalized).replaceAll("-");

        // 4. Clean up multiple dashes
        slug = MULTIPLE_DASHES.matcher(slug).replaceAll("-");

        // 5. Trim leading/trailing dashes
        slug = EDGES.matcher(slug).replaceAll("");

        return slug;
    }
}