package com.franquicias.domain;

/**
 * Projection used by the "product with the most stock per branch" query: the winner of one
 * branch, already carrying the branch it belongs to.
 *
 * <p>If several products of the same branch share the maximum stock, every tied product is
 * reported.
 *
 * @param branchId    branch that owns the product
 * @param branchName  branch name
 * @param productId   product identifier
 * @param productName product name
 * @param stock       stock of the product (the branch maximum)
 */
public record TopStockProduct(
        String branchId,
        String branchName,
        String productId,
        String productName,
        int stock
) {
}
