package com.rdc.admin.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtil {

    public static final int MAX_LENGTH = 255;

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern INVALID_CHARS = Pattern.compile("[^a-z0-9\\s-]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern DUPLICATE_DASHES = Pattern.compile("-+");
    private static final Pattern VALID_SLUG = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    private SlugUtil() {
    }

    public static String generateSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String slug = COMBINING_MARKS.matcher(normalized).replaceAll("");
        slug = slug.toLowerCase(Locale.ENGLISH).trim();
        slug = INVALID_CHARS.matcher(slug).replaceAll("");
        slug = WHITESPACE.matcher(slug).replaceAll("-");
        slug = DUPLICATE_DASHES.matcher(slug).replaceAll("-");
        slug = trimDashes(slug);

        if (slug.length() > MAX_LENGTH) {
            slug = trimDashes(slug.substring(0, MAX_LENGTH));
        }

        return slug;
    }

    public static boolean isValidSlug(String slug) {
        return slug != null
                && !slug.isBlank()
                && slug.length() <= MAX_LENGTH
                && VALID_SLUG.matcher(slug).matches();
    }

    public static String withSuffix(String baseSlug, int counter) {
        String sanitizedBase = generateSlug(baseSlug);
        if (sanitizedBase.isBlank()) {
            sanitizedBase = "item";
        }

        String suffix = "-" + counter;
        int maxBaseLength = Math.max(1, MAX_LENGTH - suffix.length());
        if (sanitizedBase.length() > maxBaseLength) {
            sanitizedBase = trimDashes(sanitizedBase.substring(0, maxBaseLength));
        }
        if (sanitizedBase.isBlank()) {
            sanitizedBase = "item";
        }

        String candidate = sanitizedBase + suffix;
        return candidate.length() > MAX_LENGTH ? candidate.substring(0, MAX_LENGTH) : candidate;
    }

    private static String trimDashes(String value) {
        String result = value;
        while (result.startsWith("-")) {
            result = result.substring(1);
        }
        while (result.endsWith("-")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
