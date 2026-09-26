package com.franquicias.service;

import com.franquicias.domain.Branch;
import com.franquicias.domain.Franchise;
import com.franquicias.domain.FranchiseDetail;
import com.franquicias.exception.ConflictException;
import com.franquicias.exception.NotFoundException;
import com.franquicias.repository.BranchRepository;
import com.franquicias.repository.FranchiseRepository;
import com.franquicias.repository.ProductRepository;
import com.franquicias.support.IdGenerator;
import com.franquicias.support.Names;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Use cases of the {@link Branch} aggregate: a branch always lives inside a franchise, so every
 * operation receives the franchise id and the repository queries enforce that relationship
 * (a branch of another franchise simply is not found).
 */
@Service
public class BranchService {

    private final FranchiseRepository franchises;
    private final BranchRepository branches;
    private final ProductRepository products;
    private final IdGenerator ids;

    public BranchService(FranchiseRepository franchises,
                         BranchRepository branches,
                         ProductRepository products,
                         IdGenerator ids) {
        this.franchises = franchises;
        this.branches = branches;
        this.products = products;
        this.ids = ids;
    }

    /** Adds a branch to an existing franchise. Branch names are unique inside the franchise. */
    public Mono<Branch> add(String franchiseId, String nombre) {
        return Mono.fromCallable(() -> Names.normalizeStrict("la sucursal", nombre))
                .flatMap(name -> requireFranchise(franchiseId)
                        .flatMap(franchise -> ensureNameIsFree(franchiseId, name, null)
                                .then(branches.insert(ids.get(), franchise.id(), name))));
    }

    public Mono<Branch> findById(String franchiseId, String branchId) {
        return branches.findById(franchiseId, branchId)
                .switchIfEmpty(Mono.error(() -> NotFoundException.branch(franchiseId, branchId)));
    }

    public Flux<Branch> findByFranchiseId(String franchiseId) {
        return requireFranchise(franchiseId).thenMany(branches.findByFranchiseId(franchiseId));
    }

    /** Renames a branch, keeping branch names unique inside its franchise. */
    public Mono<Branch> rename(String franchiseId, String branchId, String nombre) {
        return Mono.fromCallable(() -> Names.normalizeStrict("la sucursal", nombre))
                .flatMap(name -> requireFranchise(franchiseId)
                        .then(findById(franchiseId, branchId))
                        .flatMap(branch -> ensureNameIsFree(franchiseId, name, branchId)
                                .then(branches.updateName(franchiseId, branchId, name)))
                        .switchIfEmpty(Mono.error(() -> NotFoundException.branch(franchiseId, branchId))));
    }

    /** Deletes a branch; its products are removed by the {@code ON DELETE CASCADE} constraint. */
    public Mono<Void> delete(String franchiseId, String branchId) {
        return branches.delete(franchiseId, branchId)
                .flatMap(deleted -> deleted == 0L
                        ? Mono.<Void>error(NotFoundException.branch(franchiseId, branchId))
                        : Mono.empty());
    }

    /**
     * Full read model: franchise + branches + products, assembled with reactive composition.
     * {@code concatMap} is used on purpose: it keeps the order returned by the database while
     * reusing a single pooled connection per branch.
     */
    public Mono<FranchiseDetail> detail(String franchiseId) {
        return franchises.findById(franchiseId)
                .switchIfEmpty(Mono.error(() -> NotFoundException.franchise(franchiseId)))
                .flatMap(franchise -> branches.findByFranchiseId(franchiseId)
                        .concatMap(branch -> products.findByBranchId(branch.id())
                                .collectList()
                                .map(list -> new FranchiseDetail.BranchWithProducts(branch, list)))
                        .collectList()
                        .map(branchList -> new FranchiseDetail(franchise, branchList)));
    }

    private Mono<Franchise> requireFranchise(String franchiseId) {
        return franchises.findById(franchiseId)
                .switchIfEmpty(Mono.error(() -> NotFoundException.franchise(franchiseId)));
    }

    private Mono<Void> ensureNameIsFree(String franchiseId, String name, String excludingId) {
        return branches.existsByName(franchiseId, name, excludingId)
                .flatMap(exists -> exists
                        ? Mono.error(ConflictException.duplicateBranch(name))
                        : Mono.empty());
    }
}
