package com.lms.common.util;

import java.util.Locale;

/** Builds literal, case-insensitive SQL LIKE contains patterns. */
public final class LikePatternUtils {

    private LikePatternUtils() {
    }

    /** Escapes SQL LIKE metacharacters using a backslash escape character. */
    public static String containsIgnoreCase(String value) {
        String literal = value.trim()
                .toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + literal + "%";
    }
}
