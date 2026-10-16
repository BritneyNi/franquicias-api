package com.franquicias.service;

import com.franquicias.domain.Franchise;
import com.franquicias.exception.ConflictException;
import com.franquicias.exception.NotFoundException;
import com.franquicias.repository.FranchiseRepository;
import com.franquicias.support.IdGenerator;
import com.franquicias.support.Names;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Use cases of the {@link Franchise} aggregate.
 *
 * <p>All methods are non blocking and return cold publishers; no state is mutated, only new
 * immutable values are produced.
 */
@Service
public class FranchiseService {

    private final FranchiseRepository franchises;
    private final IdGenerator ids;

    public FranchiseService(FranchiseRepository franchises, IdGenerator ids) {
        this.franchises = franchises;
        this.ids = ids;
    }

    /**
     * Creates a franchise. Names are unique across the whole system.
     *
     * @param nombre raw name coming from the API
     * @return the created franchise
     */
    public Mono<Franchise> create(String nombre) {
        return Mono.fromCallable(() -> Names.normalizeStrict("la franquicia", nombre))
                .flatMap(name -> ensureNameIsFree(name, null)
                        .then(franchises.insert(ids.get(), name)));
    }

    /**
     * Renames a franchise, keeping the uniqueness rule of its scope. Renaming a franchise to the
     * exact same name is a no-op that returns the current state.
     */
    public Mono<Franchise> rename(String franchiseId, String nombre) {
        return Mono.fromCallable(() -> Names.normalizeStrict("la franquicia", nombre))
                .flatMap(name -> ensureNameIsFree(name, franchiseId)
                        .then(franchises.updateName(franchiseId, name))
                        .switchIfEmpty(Mono.error(() -> NotFoundException.franchise(franchiseId))));
    }

    public Mono<Franchise> findById(String franchiseId) {
        return franchises.findById(franchiseId)
                .switchIfEmpty(Mono.error(() -> NotFoundException.franchise(franchiseId)));
    }

    public Flux<Franchise> findAll() {
        return franchises.findAll();
    }

    public Mono<Long> count() {
        return franchises.count();
    }

    private Mono<Void> ensureNameIsFree(String name, String excludingId) {
        return franchises.existsByName(name, excludingId)
                .flatMap(exists -> exists
                        ? Mono.error(ConflictException.duplicateFranchise(name))
                        : Mono.empty());
    }
}
