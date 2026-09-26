package com.franquicias.service;

import com.franquicias.domain.Branch;
import com.franquicias.domain.Franchise;
import com.franquicias.domain.Product;
import com.franquicias.domain.TopStockProduct;
import com.franquicias.exception.ConflictException;
import com.franquicias.exception.InvalidRequestException;
import com.franquicias.exception.NotFoundException;
import com.franquicias.repository.BranchRepository;
import com.franquicias.repository.FranchiseRepository;
import com.franquicias.repository.ProductRepository;
import com.franquicias.support.IdGenerator;
import com.franquicias.support.Names;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Use cases of the {@link Product} aggregate. Products belong to a branch, and a branch to a
 * franchise, so the identifiers of the whole path are part of every signature: that is what makes
 * a product of another franchise unreachable instead of silently reachable.
 */
@Service
public class ProductService {

    private final FranchiseRepository franchises;
    private final BranchRepository branches;
    private final ProductRepository products;
    private final IdGenerator ids;

    public ProductService(FranchiseRepository franchises,
                          BranchRepository branches,
                          ProductRepository products,
                          IdGenerator ids) {
        this.franchises = franchises;
        this.branches = branches;
        this.products = products;
        this.ids = ids;
    }

    /** Adds a product to a branch of a given franchise. Names are unique inside the branch. */
    public Mono<Product> add(String franchiseId, String branchId, String nombre, Integer stock) {
        return Mono.fromCallable(() -> new NewProduct(
                        Names.normalizeStrict("el producto", nombre),
                        requireNonNegativeStock(stock)))
                .flatMap(input -> requireBranch(franchiseId, branchId)
                        .flatMap(branch -> ensureNameIsFree(branchId, input.name(), null)
                                .then(products.insert(ids.get(), branch.id(), input.name(), input.stock()))));
    }

    public Mono<Product> findById(String franchiseId, String branchId, String productId) {
        return requireBranch(franchiseId, branchId)
                .flatMap(branch -> products.findById(branch.id(), productId)
                        .switchIfEmpty(Mono.error(() -> NotFoundException.product(branchId, productId))));
    }

    public Flux<Product> findByBranch(String franchiseId, String branchId) {
        return requireBranch(franchiseId, branchId).thenMany(products.findByBranchId(branchId));
    }

    /**
     * Renames a product, keeping product names unique inside its branch.
     */
    public Mono<Product> rename(String franchiseId, String branchId, String productId, String nombre) {
        return Mono.fromCallable(() -> Names.normalizeStrict("el producto", nombre))
                .flatMap(name -> requireBranch(franchiseId, branchId)
                        .flatMap(branch -> requireProduct(branch, productId)
                                .flatMap(product -> ensureNameIsFree(branchId, name, productId)
                                        .then(products.updateName(branchId, productId, name)))));
    }

    /**
     * Replaces the stock of a product. Negative values are rejected before touching the database.
     */
    public Mono<Product> updateStock(String franchiseId, String branchId, String productId, Integer stock) {
        return Mono.fromCallable(() -> requireNonNegativeStock(stock))
                .flatMap(newStock -> requireBranch(franchiseId, branchId)
                        .flatMap(branch -> requireProduct(branch, productId)
                                .flatMap(product -> products.updateStock(branchId, productId, newStock))));
    }

    /** Removes a product from a branch. */
    public Mono<Void> remove(String franchiseId, String branchId, String productId) {
        return requireBranch(franchiseId, branchId)
                .then(products.delete(branchId, productId))
                .flatMap(deleted -> deleted == 0L
                        ? Mono.<Void>error(NotFoundException.product(branchId, productId))
                        : Mono.empty());
    }

    /**
     * Product (or products, on a tie) with the highest stock of every branch of a franchise.
     *
     * <p>A franchise with no branches at all answers {@code 200} with an empty list; an unknown
     * franchise answers {@code 404}.
     */
    public Flux<TopStockProduct> topStockPerBranch(String franchiseId) {
        return requireFranchise(franchiseId)
                .thenMany(products.findTopStockPerBranch(franchiseId));
    }

    private Mono<Franchise> requireFranchise(String franchiseId) {
        return franchises.findById(franchiseId)
                .switchIfEmpty(Mono.error(() -> NotFoundException.franchise(franchiseId)));
    }

    private Mono<Branch> requireBranch(String franchiseId, String branchId) {
        return requireFranchise(franchiseId)
                .then(branches.findById(franchiseId, branchId))
                .switchIfEmpty(Mono.error(() -> NotFoundException.branch(franchiseId, branchId)));
    }

    private Mono<Product> requireProduct(Branch branch, String productId) {
        return products.findById(branch.id(), productId)
                .switchIfEmpty(Mono.error(() -> NotFoundException.product(branch.id(), productId)));
    }

    private Mono<Void> ensureNameIsFree(String branchId, String name, String excludingId) {
        return products.existsByName(branchId, name, excludingId)
                .flatMap(exists -> exists
                        ? Mono.error(ConflictException.duplicateProduct(name))
                        : Mono.empty());
    }

    private int requireNonNegativeStock(Integer stock) {
        if (stock == null) {
            throw new InvalidRequestException("El stock es obligatorio",
                    List.of("El campo 'stock' es obligatorio"));
        }
        if (stock < 0) {
            throw new InvalidRequestException("El stock no puede ser negativo",
                    List.of("El campo 'stock' debe ser mayor o igual a 0"));
        }
        return stock;
    }

    /** Small immutable carrier so the input of {@link #add} is validated as a single value. */
    private record NewProduct(String name, int stock) {
    }
}
