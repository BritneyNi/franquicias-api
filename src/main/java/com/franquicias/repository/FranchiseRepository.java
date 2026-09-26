package com.franquicias.repository;

import com.franquicias.domain.Franchise;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Persistence port for {@link Franchise}. Implemented by an R2DBC adapter; the service layer
 * only depends on this contract, so the storage can be swapped without touching business rules.
 */
public interface FranchiseRepository {

    Mono<Franchise> insert(String id, String name);

    Mono<Franchise> findById(String id);

    Flux<Franchise> findAll();

    /** @return the franchise with the given name (case/accent insensitive) or empty. */
    Mono<Franchise> findByName(String name);

    /** @return {@code true} when some franchise already uses that name. */
    Mono<Boolean> existsByName(String name, String excludingId);

    Mono<Franchise> updateName(String id, String name);

    Mono<Long> count();
}
