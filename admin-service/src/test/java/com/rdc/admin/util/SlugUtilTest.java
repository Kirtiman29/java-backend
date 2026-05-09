package com.rdc.admin.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlugUtilTest {

    @Test
    void generatesExpectedSlug() {
        assertEquals("floral-cotton-print-design", SlugUtil.generateSlug("Floral Cotton Print Design"));
    }

    @Test
    void removesSpecialCharactersAndDuplicateDashes() {
        assertEquals("summer-sale-edition", SlugUtil.generateSlug(" Summer @@ Sale --- Edition "));
    }

    @Test
    void validatesCanonicalSlugFormat() {
        assertTrue(SlugUtil.isValidSlug("floral-design-2"));
        assertFalse(SlugUtil.isValidSlug("Floral Design"));
        assertFalse(SlugUtil.isValidSlug(""));
    }

    @Test
    void appendsSuffixWithinMaxLength() {
        String base = "a".repeat(SlugUtil.MAX_LENGTH);
        String candidate = SlugUtil.withSuffix(base, 12);

        assertTrue(candidate.endsWith("-12"));
        assertTrue(candidate.length() <= SlugUtil.MAX_LENGTH);
    }
}
