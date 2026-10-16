package com.franquicias.domain;

/**
 * A product offered by a {@link Branch}.
 *
 * @param id       generated identifier (UUID based)
 * @param branchId owning branch
 * @param name     product name, unique inside the branch
 * @param stock    available units, never negative
 */
public record Product(String id, String branchId, String name, int stock) {

    public Product {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Product id is required");
        }
        if (branchId == null || branchId.isBlank()) {
            throw new IllegalArgumentException("Product branchId is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("Product stock cannot be negative");
        }
    }
}
