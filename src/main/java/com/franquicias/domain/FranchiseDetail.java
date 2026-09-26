package com.franquicias.domain;

import java.util.List;

/**
 * Read model with the full franchise tree: franchise, its branches and each branch with its
 * products.
 *
 * @param franchise the franchise
 * @param branches  branches with their products (never {@code null}, may be empty)
 */
public record FranchiseDetail(Franchise franchise, List<BranchWithProducts> branches) {

    public record BranchWithProducts(Branch branch, List<Product> products) {
        public BranchWithProducts {
            products = products == null ? List.of() : List.copyOf(products);
        }
    }
}
