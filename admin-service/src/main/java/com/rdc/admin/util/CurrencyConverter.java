package com.rdc.admin.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for safe conversion between currency units (Cents to Dollars).
 */
public class CurrencyConverter {

    private CurrencyConverter() {
        // Private constructor to prevent instantiation of utility class
    }

    private static final BigDecimal CONVERSION_FACTOR = new BigDecimal(100);

    /**
     * Converts a Long value representing cents into a BigDecimal representing dollars.
     * * @param cents The amount in cents (e.g., 1500L)
     * @return The amount in dollars (e.g., 15.00)
     */
    public static BigDecimal convertCentsToDollars(Long cents) {
        if (cents == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(cents)
                .divide(CONVERSION_FACTOR, 2, RoundingMode.HALF_UP);
    }
}