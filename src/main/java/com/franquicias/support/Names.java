package com.franquicias.support;

import java.util.Locale;
import java.util.function.UnaryOperator;

/**
 * Pure helper functions for the business rules around user supplied names.
 *
 * <p>Every method is stateless and side effect free, which keeps the rules easy to unit test
 * and safe to reuse from any layer.
 */
public final class Names {

    /** Maximum length accepted for a franchise, branch or product name. */
    public static final int MAX_LENGTH = 120;

    private Names() {
    }

    /**
     * Trims the name and collapses internal whitespace runs into a single space.
     *
     * @param name raw name, may be {@code null}
     * @return normalized name, {@code null} when the input was {@code null}
     */
    public static String normalize(String name) {
        return name == null ? null : name.trim().replaceAll("\\s+", " ");
    }

    /**
     * Normalizes and validates a name.
     *
     * @param entity human readable entity name used in the error message
     * @param name   raw name
     * @return normalized name
     * @throws IllegalArgumentException when the name is blank or longer than {@link #MAX_LENGTH}
     */
    public static String normalizeStrict(String entity, String name) {
        String normalized = normalize(name);
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException("El nombre de " + entity + " es obligatorio");
        }
        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "El nombre de " + entity + " no puede superar " + MAX_LENGTH + " caracteres");
        }
        return normalized;
    }

    /**
     * Case and accent insensitive key used to compare names for uniqueness, so that
     * {@code "Cafe"} and {@code "café"} are considered the same name.
     *
     * @param name already normalized name
     * @return comparison key
     */
    public static String key(String name) {
        String normalized = normalize(name);
        if (normalized == null) {
            return null;
        }
        String decomposed = java.text.Normalizer.normalize(normalized, java.text.Normalizer.Form.NFD);
        return decomposed
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT);
    }

    /** Composes {@code key(apply(value))} in a single functional step. */
    public static UnaryOperator<String> keyWith(UnaryOperator<String> normalizer) {
        return value -> key(normalizer.apply(value));
    }
}
