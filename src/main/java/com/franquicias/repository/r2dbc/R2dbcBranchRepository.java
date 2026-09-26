package com.franquicias.repository.r2dbc;

import com.franquicias.domain.Branch;
import com.franquicias.repository.BranchRepository;
import com.franquicias.support.Names;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** R2DBC/MySQL implementation of {@link BranchRepository}. */
@Repository
public class R2dbcBranchRepository implements BranchRepository {

    private static final String COLUMNS = "id, franchise_id, name, name_key";

    private static final String INSERT = """
            INSERT INTO branches (id, franchise_id, name, name_key)
            VALUES (:id, :franchiseId, :name, :nameKey)
            """;

    private static final String FIND_BY_ID = """
            SELECT %s FROM branches WHERE franchise_id = :franchiseId AND id = :id
            """.formatted(COLUMNS);

    private static final String FIND_BY_FRANCHISE = """
            SELECT %s FROM branches WHERE franchise_id = :franchiseId ORDER BY name
            """.formatted(COLUMNS);

    private static final String FIND_BY_NAME = """
            SELECT %s FROM branches WHERE franchise_id = :franchiseId AND name_key = :nameKey
            """.formatted(COLUMNS);

    private static final String UPDATE_NAME = """
            UPDATE branches SET name = :name, name_key = :nameKey
            WHERE franchise_id = :franchiseId AND id = :id
            """;

    private static final String DELETE = """
            DELETE FROM branches WHERE franchise_id = :franchiseId AND id = :id
            """;

    private final DatabaseClient client;

    public R2dbcBranchRepository(DatabaseClient client) {
        this.client = client;
    }

    @Override
    public Mono<Branch> insert(String id, String franchiseId, String name) {
        return client.sql(INSERT)
                .bind("id", id)
                .bind("franchiseId", franchiseId)
                .bind("name", name)
                .bind("nameKey", Names.key(name))
                .fetch().rowsUpdated()
                .thenReturn(new Branch(id, franchiseId, name));
    }

    @Override
    public Mono<Branch> findById(String franchiseId, String id) {
        return client.sql(FIND_BY_ID)
                .bind("franchiseId", franchiseId)
                .bind("id", id)
                .map(RowMappers.BRANCH)
                .one();
    }

    @Override
    public Flux<Branch> findByFranchiseId(String franchiseId) {
        return client.sql(FIND_BY_FRANCHISE)
                .bind("franchiseId", franchiseId)
                .map(RowMappers.BRANCH)
                .all();
    }

    @Override
    public Mono<Branch> findByName(String franchiseId, String name) {
        return client.sql(FIND_BY_NAME)
                .bind("franchiseId", franchiseId)
                .bind("nameKey", Names.key(name))
                .map(RowMappers.BRANCH)
                .one();
    }

    @Override
    public Mono<Boolean> existsByName(String franchiseId, String name, String excludingId) {
        String sql = excludingId == null
                ? "SELECT COUNT(*) FROM branches WHERE franchise_id = :franchiseId AND name_key = :nameKey"
                : """
                  SELECT COUNT(*) FROM branches
                  WHERE franchise_id = :franchiseId AND name_key = :nameKey AND id <> :excludingId
                  """;

        var spec = client.sql(sql)
                .bind("franchiseId", franchiseId)
                .bind("nameKey", Names.key(name));
        if (excludingId != null) {
            spec = spec.bind("excludingId", excludingId);
        }
        return spec.map((row, metadata) -> row.get(0, Long.class)).one().defaultIfEmpty(0L).map(count -> count > 0);
    }

    @Override
    public Mono<Branch> updateName(String franchiseId, String id, String name) {
        return client.sql(UPDATE_NAME)
                .bind("franchiseId", franchiseId)
                .bind("id", id)
                .bind("name", name)
                .bind("nameKey", Names.key(name))
                .fetch().rowsUpdated()
                .then(findById(franchiseId, id));
    }

    @Override
    public Mono<Long> delete(String franchiseId, String id) {
        return client.sql(DELETE)
                .bind("franchiseId", franchiseId)
                .bind("id", id)
                .fetch().rowsUpdated();
    }

    @Override
    public Mono<Long> countByFranchiseId(String franchiseId) {
        return client.sql("SELECT COUNT(*) FROM branches WHERE franchise_id = :franchiseId")
                .bind("franchiseId", franchiseId)
                .map((row, metadata) -> row.get(0, Long.class))
                .one()
                .defaultIfEmpty(0L);
    }
}
