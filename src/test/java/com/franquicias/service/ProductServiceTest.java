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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests of the business rules, written against hand made in memory implementations of the
 * repository ports: no Spring context, no database and no mocking framework. The services are
 * pure functions over those ports, which is exactly what makes this possible.
 */
class ProductServiceTest {

    /** Shared data so the three ports can resolve references between them, like the real DB does. */
    private static final class Data {
        final Map<String, Franchise> franchises = new HashMap<>();
        final Map<String, Branch> branches = new HashMap<>();
        final Map<String, Product> products = new HashMap<>();
        final AtomicInteger sequence = new AtomicInteger();

        String nextId() {
            return "id-" + sequence.incrementAndGet();
        }

        boolean belongsToFranchise(String branchId, String franchiseId) {
            Branch branch = branches.get(branchId);
            return branch != null && branch.franchiseId().equals(franchiseId);
        }

        String branchName(String branchId) {
            Branch branch = branches.get(branchId);
            return branch == null ? "" : branch.name();
        }
    }

    private static final class FakeFranchiseRepository implements FranchiseRepository {
        private final Data data;

        FakeFranchiseRepository(Data data) {
            this.data = data;
        }

        @Override
        public Mono<Franchise> insert(String id, String name) {
            Franchise franchise = new Franchise(id, name);
            data.franchises.put(id, franchise);
            return Mono.just(franchise);
        }

        @Override
        public Mono<Franchise> findById(String id) {
            return Mono.justOrEmpty(data.franchises.get(id));
        }

        @Override
        public Flux<Franchise> findAll() {
            return Flux.fromIterable(data.franchises.values());
        }

        @Override
        public Mono<Franchise> findByName(String name) {
            return Mono.justOrEmpty(data.franchises.values().stream()
                    .filter(f -> Names.key(f.name()).equals(Names.key(name)))
                    .findFirst());
        }

        @Override
        public Mono<Boolean> existsByName(String name, String excludingId) {
            return findByName(name).map(f -> !f.id().equals(excludingId)).defaultIfEmpty(false);
        }

        @Override
        public Mono<Franchise> updateName(String id, String name) {
            return findById(id)
                    .map(current -> new Franchise(id, name))
                    .doOnNext(updated -> data.franchises.put(id, updated));
        }

        @Override
        public Mono<Long> count() {
            return Mono.just((long) data.franchises.size());
        }
    }

    private static final class FakeBranchRepository implements BranchRepository {
        private final Data data;

        FakeBranchRepository(Data data) {
            this.data = data;
        }

        @Override
        public Mono<Branch> insert(String id, String franchiseId, String name) {
            Branch branch = new Branch(id, franchiseId, name);
            data.branches.put(id, branch);
            return Mono.just(branch);
        }

        @Override
        public Mono<Branch> findById(String franchiseId, String id) {
            return Mono.justOrEmpty(data.branches.get(id))
                    .filter(b -> b.franchiseId().equals(franchiseId));
        }

        @Override
        public Flux<Branch> findByFranchiseId(String franchiseId) {
            return Flux.fromIterable(data.branches.values().stream()
                    .filter(b -> b.franchiseId().equals(franchiseId))
                    .toList());
        }

        @Override
        public Mono<Branch> findByName(String franchiseId, String name) {
            return Mono.justOrEmpty(data.branches.values().stream()
                    .filter(b -> b.franchiseId().equals(franchiseId))
                    .filter(b -> Names.key(b.name()).equals(Names.key(name)))
                    .findFirst());
        }

        @Override
        public Mono<Boolean> existsByName(String franchiseId, String name, String excludingId) {
            return findByName(franchiseId, name).map(b -> !b.id().equals(excludingId)).defaultIfEmpty(false);
        }

        @Override
        public Mono<Branch> updateName(String franchiseId, String id, String name) {
            return findById(franchiseId, id)
                    .map(current -> new Branch(id, franchiseId, name))
                    .doOnNext(updated -> data.branches.put(id, updated));
        }

        @Override
        public Mono<Long> delete(String franchiseId, String id) {
            return findById(franchiseId, id)
                    .map(branch -> {
                        data.branches.remove(branch.id());
                        data.products.values().removeIf(p -> p.branchId().equals(branch.id()));
                        return 1L;
                    })
                    .defaultIfEmpty(0L);
        }

        @Override
        public Mono<Long> countByFranchiseId(String franchiseId) {
            return findByFranchiseId(franchiseId).count();
        }
    }

    private static final class FakeProductRepository implements ProductRepository {
        private final Data data;

        FakeProductRepository(Data data) {
            this.data = data;
        }

        @Override
        public Mono<Product> insert(String id, String branchId, String name, int stock) {
            Product product = new Product(id, branchId, name, stock);
            data.products.put(id, product);
            return Mono.just(product);
        }

        @Override
        public Mono<Product> findById(String branchId, String id) {
            return Mono.justOrEmpty(data.products.get(id)).filter(p -> p.branchId().equals(branchId));
        }

        @Override
        public Flux<Product> findByBranchId(String branchId) {
            return Flux.fromIterable(data.products.values().stream()
                    .filter(p -> p.branchId().equals(branchId))
                    .toList());
        }

        @Override
        public Mono<Product> findByName(String branchId, String name) {
            return Mono.justOrEmpty(data.products.values().stream()
                    .filter(p -> p.branchId().equals(branchId))
                    .filter(p -> Names.key(p.name()).equals(Names.key(name)))
                    .findFirst());
        }

        @Override
        public Mono<Boolean> existsByName(String branchId, String name, String excludingId) {
            return findByName(branchId, name).map(p -> !p.id().equals(excludingId)).defaultIfEmpty(false);
        }

        @Override
        public Mono<Product> updateName(String branchId, String id, String name) {
            return findById(branchId, id)
                    .map(current -> new Product(id, branchId, name, current.stock()))
                    .doOnNext(updated -> data.products.put(id, updated));
        }

        @Override
        public Mono<Product> updateStock(String branchId, String id, int stock) {
            return findById(branchId, id)
                    .map(current -> new Product(id, branchId, current.name(), stock))
                    .doOnNext(updated -> data.products.put(id, updated));
        }

        @Override
        public Mono<Long> delete(String branchId, String id) {
            return Mono.just(data.products.remove(id) == null ? 0L : 1L);
        }

        @Override
        public Mono<Long> countByBranchId(String branchId) {
            return findByBranchId(branchId).count();
        }

        @Override
        public Flux<TopStockProduct> findTopStockPerBranch(String franchiseId) {
            // Pure stream pipeline mirroring TOP_STOCK_PER_BRANCH in the R2DBC adapter: keep the
            // products whose stock equals the maximum of *their own branch*, so every branch of the
            // franchise contributes one row (plus any tie) and no branch is collapsed into another.
            Map<String, Integer> maxPerBranch = data.products.values().stream()
                    .filter(p -> data.belongsToFranchise(p.branchId(), franchiseId))
                    .collect(Collectors.toMap(Product::branchId, Product::stock, Math::max));
            return Flux.fromIterable(data.products.values().stream()
                    .filter(p -> maxPerBranch.containsKey(p.branchId())
                            && p.stock() == maxPerBranch.get(p.branchId()))
                    .map(p -> new TopStockProduct(p.branchId(), data.branchName(p.branchId()),
                            p.id(), p.name(), p.stock()))
                    .sorted(Comparator.comparingInt(TopStockProduct::stock).reversed()
                            .thenComparing(TopStockProduct::branchName)
                            .thenComparing(TopStockProduct::productName))
                    .toList());
        }
    }

    private final Data data = new Data();
    private final IdGenerator ids = data::nextId;
    private final ProductService service = new ProductService(
            new FakeFranchiseRepository(data),
            new FakeBranchRepository(data),
            new FakeProductRepository(data),
            ids);

    private Franchise franchise;
    private Branch branch;

    @BeforeEach
    void setUp() {
        franchise = new Franchise("f-1", "Krispy Kreme");
        data.franchises.put(franchise.id(), franchise);
        branch = new Branch("s-1", franchise.id(), "Centro");
        data.branches.put(branch.id(), branch);
    }

    private Product seedProduct(String name, int stock) {
        Product product = new Product(data.nextId(), branch.id(), name, stock);
        data.products.put(product.id(), product);
        return product;
    }

    @Test
    @DisplayName("crea un producto normalizando el nombre y usando el generador de ids")
    void shouldCreateProduct() {
        StepVerifier.create(service.add(franchise.id(), branch.id(), "  Donas   glaseadas ", 12))
                .assertNext(product -> {
                    assertThat(product.id()).isNotBlank();
                    assertThat(product.name()).isEqualTo("Donas glaseadas");
                    assertThat(product.stock()).isEqualTo(12);
                    assertThat(product.branchId()).isEqualTo(branch.id());
                    assertThat(data.products).containsEntry(product.id(), product);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("no crea el producto si la sucursal pertenece a otra franquicia")
    void shouldFailWhenBranchIsNotInFranchise() {
        Franchise otra = new Franchise("f-2", "Subway");
        data.franchises.put(otra.id(), otra);
        Branch ajena = new Branch("s-2", otra.id(), "Centro");
        data.branches.put(ajena.id(), ajena);

        StepVerifier.create(service.add(franchise.id(), ajena.id(), "Donas", 1))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("rechaza un stock negativo antes de tocar la base de datos")
    void shouldRejectNegativeStock() {
        StepVerifier.create(service.add(franchise.id(), branch.id(), "Donas", -1))
                .expectError(InvalidRequestException.class)
                .verify();

        assertThat(data.products).isEmpty();
    }

    @Test
    @DisplayName("rechaza un nombre de producto repetido dentro de la sucursal")
    void shouldRejectDuplicatedName() {
        seedProduct("Donas", 5);

        StepVerifier.create(service.add(franchise.id(), branch.id(), "donas", 3))
                .expectError(ConflictException.class)
                .verify();
    }

    @Test
    @DisplayName("actualiza el stock devolviendo el producto con el valor nuevo")
    void shouldUpdateStock() {
        Product product = seedProduct("Donas", 5);

        StepVerifier.create(service.updateStock(franchise.id(), branch.id(), product.id(), 99))
                .assertNext(updated -> {
                    assertThat(updated.stock()).isEqualTo(99);
                    assertThat(updated.name()).isEqualTo("Donas");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("elimina el producto y falla si ya no existe")
    void shouldRemoveProduct() {
        Product product = seedProduct("Donas", 5);

        StepVerifier.create(service.remove(franchise.id(), branch.id(), product.id()))
                .verifyComplete();

        StepVerifier.create(service.remove(franchise.id(), branch.id(), product.id()))
                .expectError(NotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("renombra un producto manteniendo la unicidad dentro de la sucursal")
    void shouldRenameProduct() {
        Product product = seedProduct("Donas", 5);
        seedProduct("Cafe", 2);

        StepVerifier.create(service.rename(franchise.id(), branch.id(), product.id(), "Donut"))
                .assertNext(renamed -> assertThat(renamed.name()).isEqualTo("Donut"))
                .verifyComplete();

        StepVerifier.create(service.rename(franchise.id(), branch.id(), product.id(), "Cafe"))
                .expectError(ConflictException.class)
                .verify();
    }

    @Test
    @DisplayName("devuelve el producto con mas stock de cada sucursal de la franquicia")
    void shouldReturnTopStockPerBranch() {
        seedProduct("Donas", 10);
        Product cafe = seedProduct("Cafe", 50);
        seedProduct("Jugo", 3);

        Branch norte = new Branch("s-2", franchise.id(), "Norte");
        data.branches.put(norte.id(), norte);
        Product empanada = new Product(data.nextId(), norte.id(), "Empanada", 20);
        data.products.put(empanada.id(), empanada);

        // Otra franquicia con un stock enorme: no debe aparecer en el resultado.
        Franchise otra = new Franchise("f-2", "Subway");
        data.franchises.put(otra.id(), otra);
        Branch ajena = new Branch("s-3", otra.id(), "Centro");
        data.branches.put(ajena.id(), ajena);
        Product susu = new Product(data.nextId(), ajena.id(), "Susu", 1000);
        data.products.put(susu.id(), susu);

        StepVerifier.create(service.topStockPerBranch(franchise.id()).collectList())
                .assertNext(result -> assertThat(result)
                        .containsExactly(
                                new TopStockProduct(branch.id(), "Centro", cafe.id(), "Cafe", 50),
                                new TopStockProduct(norte.id(), "Norte", empanada.id(), "Empanada", 20)))
                .verifyComplete();
    }
}
