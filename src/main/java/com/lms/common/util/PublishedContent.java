package com.lms.common.util;

import java.util.Locale;

/** Keeps known legacy seed/editor prompts out of published descriptions. */
public final class PublishedContent {
    private PublishedContent() {}

    public static String description(String title, String text) {
        if (text == null) return null;
        String normalized = text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        boolean editorPrompt = normalized.contains("do you want to support both video lessons")
                || normalized.contains("should we include drag-and-drop functionality")
                || normalized.startsWith("lorem ipsum")
                || normalized.equals("test description")
                || normalized.equals("sample description");
        boolean unrelatedSeed = title != null && title.toLowerCase(Locale.ROOT).contains("angular")
                && normalized.contains("mastering illustrator");
        return editorPrompt || unrelatedSeed ? null : text;
    }
}
