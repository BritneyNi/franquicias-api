package com.franquicias.repository;

import com.franquicias.domain.Branch;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Persistence port for {@link Branch}, always scoped by the owning franchise. */
public interface BranchRepository {

    Mono<Branch> insert(String id, String franchiseId, String name);

    /** @return the branch only when it exists and belongs to the given franchise. */
    Mono<Branch> findById(String franchiseId, String id);

    Flux<Branch> findByFranchiseId(String franchiseId);

    /** @return the branch with that name inside the franchise (case/accent insensitive) or empty. */
    Mono<Branch> findByName(String franchiseId, String name);

    /** @return {@code true} when the franchise already has a branch with that name. */
    Mono<Boolean> existsByName(String franchiseId, String name, String excludingId);

    Mono<Branch> updateName(String franchiseId, String id, String name);

    /** @return number of deleted rows (0 when the branch does not exist). */
    Mono<Long> delete(String franchiseId, String id);

    Mono<Long> countByFranchiseId(String franchiseId);
}
