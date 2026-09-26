package com.franquicias.domain;

/**
 * Franchise aggregate root: a named business that owns a set of branches.
 *
 * <p>Every model type in the domain is an immutable Java {@code record}: there is no shared
 * mutable state, so instances can be created, shared and combined freely across reactive
 * pipelines (Mono/Flux).
 *
 * @param id   generated identifier (UUID based)
 * @param name franchise name, unique across the system
 */
public record Franchise(String id, String name) {

    public Franchise {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Franchise id is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Franchise name is required");
        }
    }
}
