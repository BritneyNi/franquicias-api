package com.franquicias.repository.r2dbc;

import com.franquicias.domain.Product;
import com.franquicias.domain.TopStockProduct;
import com.franquicias.repository.ProductRepository;
import com.franquicias.support.Names;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** R2DBC/MySQL implementation of {@link ProductRepository}. */
@Repository
public class R2dbcProductRepository implements ProductRepository {

    private static final String COLUMNS = "id, branch_id, name, stock";

    private static final String INSERT = """
            INSERT INTO products (id, branch_id, name, name_key, stock)
            VALUES (:id, :branchId, :name, :nameKey, :stock)
            """;

    private static final String FIND_BY_ID = """
            SELECT %s FROM products WHERE branch_id = :branchId AND id = :id
            """.formatted(COLUMNS);

    private static final String FIND_BY_BRANCH = """
            SELECT %s FROM products WHERE branch_id = :branchId ORDER BY name
            """.formatted(COLUMNS);

    private static final String FIND_BY_NAME = """
            SELECT %s FROM products WHERE branch_id = :branchId AND name_key = :nameKey
            """.formatted(COLUMNS);

    private static final String UPDATE_NAME = """
            UPDATE products SET name = :name, name_key = :nameKey
            WHERE branch_id = :branchId AND id = :id
            """;

    private static final String UPDATE_STOCK = """
            UPDATE products SET stock = :stock WHERE branch_id = :branchId AND id = :id
            """;

    private static final String DELETE = """
            DELETE FROM products WHERE branch_id = :branchId AND id = :id
            """;

    /**
     * Winner of every branch in one round trip. The correlated subquery keeps the maximum
     * comparison inside the database and reports every product tied for the maximum, which is
     * what the acceptance criteria ask for.
     */
    private static final String TOP_STOCK_PER_BRANCH = """
            SELECT b.id        AS branch_id,
                   b.name      AS branch_name,
                   p.id        AS product_id,
                   p.name      AS product_name,
                   p.stock     AS stock
            FROM franchises f
                     JOIN branches b ON b.franchise_id = f.id
                     JOIN products p ON p.branch_id = b.id
            WHERE f.id = :franchiseId
              AND p.stock = (SELECT MAX(p2.stock) FROM products p2 WHERE p2.branch_id = b.id)
            ORDER BY p.stock DESC, b.name ASC, p.name ASC
            """;

    private final DatabaseClient client;

    public R2dbcProductRepository(DatabaseClient client) {
        this.client = client;
    }

    @Override
    public Mono<Product> insert(String id, String branchId, String name, int stock) {
        return client.sql(INSERT)
                .bind("id", id)
                .bind("branchId", branchId)
                .bind("name", name)
                .bind("nameKey", Names.key(name))
                .bind("stock", stock)
                .fetch().rowsUpdated()
                .thenReturn(new Product(id, branchId, name, stock));
    }

    @Override
    public Mono<Product> findById(String branchId, String id) {
        return client.sql(FIND_BY_ID)
                .bind("branchId", branchId)
                .bind("id", id)
                .map(RowMappers.PRODUCT)
                .one();
    }

    @Override
    public Flux<Product> findByBranchId(String branchId) {
        return client.sql(FIND_BY_BRANCH)
                .bind("branchId", branchId)
                .map(RowMappers.PRODUCT)
                .all();
    }

    @Override
    public Mono<Product> findByName(String branchId, String name) {
        return client.sql(FIND_BY_NAME)
                .bind("branchId", branchId)
                .bind("nameKey", Names.key(name))
                .map(RowMappers.PRODUCT)
                .one();
    }

    @Override
    public Mono<Boolean> existsByName(String branchId, String name, String excludingId) {
        String sql = excludingId == null
                ? "SELECT COUNT(*) FROM products WHERE branch_id = :branchId AND name_key = :nameKey"
                : """
                  SELECT COUNT(*) FROM products
                  WHERE branch_id = :branchId AND name_key = :nameKey AND id <> :excludingId
                  """;

        var spec = client.sql(sql)
                .bind("branchId", branchId)
                .bind("nameKey", Names.key(name));
        if (excludingId != null) {
            spec = spec.bind("excludingId", excludingId);
        }
        return spec.map((row, metadata) -> row.get(0, Long.class)).one().defaultIfEmpty(0L).map(count -> count > 0);
    }

    @Override
    public Mono<Product> updateName(String branchId, String id, String name) {
        return client.sql(UPDATE_NAME)
                .bind("branchId", branchId)
                .bind("id", id)
                .bind("name", name)
                .bind("nameKey", Names.key(name))
                .fetch().rowsUpdated()
                .then(findById(branchId, id));
    }

    @Override
    public Mono<Product> updateStock(String branchId, String id, int stock) {
        return client.sql(UPDATE_STOCK)
                .bind("branchId", branchId)
                .bind("id", id)
                .bind("stock", stock)
                .fetch().rowsUpdated()
                .then(findById(branchId, id));
    }

    @Override
    public Mono<Long> delete(String branchId, String id) {
        return client.sql(DELETE)
                .bind("branchId", branchId)
                .bind("id", id)
                .fetch().rowsUpdated();
    }

    @Override
    public Mono<Long> countByBranchId(String branchId) {
        return client.sql("SELECT COUNT(*) FROM products WHERE branch_id = :branchId")
                .bind("branchId", branchId)
                .map((row, metadata) -> row.get(0, Long.class))
                .one()
                .defaultIfEmpty(0L);
    }

    @Override
    public Flux<TopStockProduct> findTopStockPerBranch(String franchiseId) {
        return client.sql(TOP_STOCK_PER_BRANCH)
                .bind("franchiseId", franchiseId)
                .map(RowMappers.TOP_STOCK)
                .all();
    }
}
