package com.example.QuickFix.service;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Compares place names the way people write them: ignoring case, punctuation,
 * spacing, and words such as "Urban" or "GPO", and mapping well-known alternative
 * spellings to the official name.
 */
public final class PlaceNames {

    /** Old and alternative spellings, mapped to the official name. Extend as needed. */
    static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("bangalore", "bengaluru"),
            Map.entry("bombay", "mumbai"),
            Map.entry("calcutta", "kolkata"),
            Map.entry("madras", "chennai"),
            Map.entry("poona", "pune"),
            Map.entry("mysore", "mysuru"),
            Map.entry("gurgaon", "gurugram"),
            Map.entry("trivandrum", "thiruvananthapuram"),
            Map.entry("cochin", "kochi"),
            Map.entry("baroda", "vadodara"),
            Map.entry("benares", "varanasi"),
            Map.entry("banaras", "varanasi"),
            Map.entry("allahabad", "prayagraj"),
            Map.entry("pondicherry", "puducherry"),
            Map.entry("orissa", "odisha"));

    /** Words that describe a district or an office rather than the place itself. */
    private static final Set<String> DROP_WORDS = Set.of(
            "urban", "rural", "district", "city", "north", "south", "east", "west",
            "gpo", "hpo", "bo", "so", "s", "o", "b", "h", "sorting", "mdg", "rms", "ms");

    private PlaceNames() {
    }

    /** The comparison form of a name, e.g. "Bangalore Urban" -> "bengaluru". */
    public static String canonical(String name) {
        if (name == null) {
            return "";
        }
        String text = name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        StringBuilder kept = new StringBuilder();
        for (String word : text.split(" ")) {
            if (word.isEmpty() || DROP_WORDS.contains(word)) {
                continue;
            }
            if (kept.length() > 0) {
                kept.append(' ');
            }
            kept.append(word);
        }
        String result = kept.toString();
        return ALIASES.getOrDefault(result, result);
    }
}
