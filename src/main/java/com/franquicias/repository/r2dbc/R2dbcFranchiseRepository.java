package com.franquicias.repository.r2dbc;

import com.franquicias.domain.Franchise;
import com.franquicias.repository.FranchiseRepository;
import com.franquicias.support.Names;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * R2DBC/MySQL implementation of {@link FranchiseRepository} built on {@link DatabaseClient}.
 *
 * <p>Every method returns a cold {@link Mono}/{@link Flux}: no connection is acquired and no
 * query is executed until the pipeline is subscribed to.
 */
@Repository
public class R2dbcFranchiseRepository implements FranchiseRepository {

    private static final String COLUMNS = "id, name, name_key";

    private static final String INSERT = """
            INSERT INTO franchises (id, name, name_key)
            VALUES (:id, :name, :nameKey)
            """;

    private static final String FIND_BY_ID = """
            SELECT %s FROM franchises WHERE id = :id
            """.formatted(COLUMNS);

    private static final String FIND_ALL = """
            SELECT %s FROM franchises ORDER BY name
            """.formatted(COLUMNS);

    private static final String FIND_BY_NAME = """
            SELECT %s FROM franchises WHERE name_key = :nameKey
            """.formatted(COLUMNS);

    private static final String COUNT = "SELECT COUNT(*) FROM franchises";

    private final DatabaseClient client;

    public R2dbcFranchiseRepository(DatabaseClient client) {
        this.client = client;
    }

    @Override
    public Mono<Franchise> insert(String id, String name) {
        return client.sql(INSERT)
                .bind("id", id)
                .bind("name", name)
                .bind("nameKey", Names.key(name))
                .fetch().rowsUpdated()
                .thenReturn(new Franchise(id, name));
    }

    @Override
    public Mono<Franchise> findById(String id) {
        return client.sql(FIND_BY_ID)
                .bind("id", id)
                .map(RowMappers.FRANCHISE)
                .one();
    }

    @Override
    public Flux<Franchise> findAll() {
        return client.sql(FIND_ALL).map(RowMappers.FRANCHISE).all();
    }

    @Override
    public Mono<Franchise> findByName(String name) {
        return client.sql(FIND_BY_NAME)
                .bind("nameKey", Names.key(name))
                .map(RowMappers.FRANCHISE)
                .one();
    }

    @Override
    public Mono<Boolean> existsByName(String name, String excludingId) {
        // The "excludingId" clause is appended only when needed: it keeps the query index friendly
        // and avoids binding null parameters.
        String sql = excludingId == null
                ? "SELECT COUNT(*) FROM franchises WHERE name_key = :nameKey"
                : "SELECT COUNT(*) FROM franchises WHERE name_key = :nameKey AND id <> :excludingId";

        var spec = client.sql(sql).bind("nameKey", Names.key(name));
        if (excludingId != null) {
            spec = spec.bind("excludingId", excludingId);
        }
        return spec.map((row, metadata) -> row.get(0, Long.class)).one().defaultIfEmpty(0L).map(count -> count > 0);
    }

    @Override
    public Mono<Franchise> updateName(String id, String name) {
        return client.sql("UPDATE franchises SET name = :name, name_key = :nameKey WHERE id = :id")
                .bind("id", id)
                .bind("name", name)
                .bind("nameKey", Names.key(name))
                .fetch().rowsUpdated()
                .then(findById(id));
    }

    @Override
    public Mono<Long> count() {
        return client.sql(COUNT).map((row, metadata) -> row.get(0, Long.class)).one().defaultIfEmpty(0L);
    }
}
