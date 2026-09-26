package com.franquicias.domain;

/**
 * A branch that belongs to a {@link Franchise} and offers a set of products.
 *
 * @param id          generated identifier (UUID based)
 * @param franchiseId owner franchise
 * @param name        branch name, unique inside the franchise
 */
public record Branch(String id, String franchiseId, String name) {

    public Branch {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Branch id is required");
        }
        if (franchiseId == null || franchiseId.isBlank()) {
            throw new IllegalArgumentException("Branch franchiseId is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Branch name is required");
        }
    }
}
