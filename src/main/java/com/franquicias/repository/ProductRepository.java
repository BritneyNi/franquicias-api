package com.franquicias.repository;

import com.franquicias.domain.Product;
import com.franquicias.domain.TopStockProduct;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Persistence port for {@link Product}, always scoped by the owning branch. */
public interface ProductRepository {

    Mono<Product> insert(String id, String branchId, String name, int stock);

    Mono<Product> findById(String branchId, String id);

    Flux<Product> findByBranchId(String branchId);

    /** @return the product with that name inside the branch (case/accent insensitive) or empty. */
    Mono<Product> findByName(String branchId, String name);

    /** @return {@code true} when the branch already offers a product with that name. */
    Mono<Boolean> existsByName(String branchId, String name, String excludingId);

    Mono<Product> updateName(String branchId, String id, String name);

    Mono<Product> updateStock(String branchId, String id, int stock);

    /** @return number of deleted rows (0 when the product does not exist). */
    Mono<Long> delete(String branchId, String id);

    Mono<Long> countByBranchId(String branchId);

    /**
     * Resolves the product (or products, on a tie) holding the highest stock of every branch
     * of the given franchise.
     *
     * @param franchiseId franchise to inspect
     * @return stream ordered by stock descending, then branch name and product name
     */
    Flux<TopStockProduct> findTopStockPerBranch(String franchiseId);
}
