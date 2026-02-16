package com.example.pulse.common.tags;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public final class TagUtil {
    private TagUtil() {}

    /**
     * Canonical key for a tag-map: sort keys, escape '=' and ','.
     * Example: env=prod,region=kr
     */
    public static String canonicalize(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) return "";

        StringJoiner joiner = new StringJoiner(",");
        tags.entrySet().stream()
            .filter(e -> e.getKey() != null && e.getValue() != null)
            .sorted(Comparator.comparing(Map.Entry::getKey))
            .forEach(e -> joiner.add(escape(e.getKey()) + "=" + escape(e.getValue())));

        return joiner.toString();
    }

    public static String tagsKey(Map<String, String> tags) {
        String canonical = canonicalize(tags);
        if (canonical.isEmpty()) return "-";
        return shortSha256Hex(canonical);
    }

    private static String escape(String s) {
        return s.replace("=", "%3D").replace(",", "%2C");
    }

    private static String shortSha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            // shorten to 16 hex chars (64 bits) for v1
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                sb.append(Character.forDigit((digest[i] >> 4) & 0xF, 16));
                sb.append(Character.forDigit(digest[i] & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

