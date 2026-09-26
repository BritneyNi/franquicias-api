package com.franquicias.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End to end coverage of every acceptance criterion, exercised through the real HTTP stack
 * (functional routes, validation, R2DBC persistence and the global error handler).
 *
 * <p>Each test class gets its own in memory database so the tests stay independent.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.r2dbc.url=r2dbc:h2:mem:///test-api;DB_CLOSE_DELAY=-1")
class FranchiseApiIntegrationTest extends AbstractApiTest {

    // ------------------------------------------------------------------ franquicias (CA 2)

    @Test
    @DisplayName("CA 2 - POST /api/franquicias crea una franquicia y devuelve 201 con Location")
    void shouldCreateFranchise() {
        ApiResponse response = exchange(postJson("/api/franquicias", """
                {"nombre": "Krispy Kreme"}
                """));

        response.hasStatus(201);
        assertThat(response.text("id")).isNotBlank();
        assertThat(response.text("nombre")).isEqualTo("Krispy Kreme");
        assertThat(response.header("Location")).contains("/api/franquicias/" + response.text("id"));
    }

    @Test
    @DisplayName("CA 2 - el nombre de la franquicia se normaliza (espacios sobrantes)")
    void shouldNormalizeFranchiseName() {
        exchange(postJson("/api/franquicias", """
                {"nombre": "  Burger   King  "}
                """))
                .hasStatus(201)
                .textIs("nombre", "Burger King");
    }

    @Test
    @DisplayName("Validacion - un nombre vacio responde 400 con el detalle de los campos")
    void shouldRejectBlankFranchiseName() {
        ApiResponse response = exchange(postJson("/api/franquicias", """
                {"nombre": "   "}
                """));

        response.hasStatus(400);
        assertThat(response.text("title")).isEqualTo("Solicitud inválida");
        assertThat(response.text("status")).isEqualTo("400");
        assertThat(response.body().path("errors")).hasSize(1);
        assertThat(response.body().path("errors").get(0).asText()).contains("nombre");
    }

    @Test
    @DisplayName("Validacion - un cuerpo vacio responde 400 y no 200")
    void shouldRejectEmptyBody() {
        exchange(postRawJson("/api/franquicias", "")).hasStatus(400);
    }

    @Test
    @DisplayName("Validacion - un JSON mal formado responde 400")
    void shouldRejectMalformedJson() {
        exchange(postRawJson("/api/franquicias", "{ esto no es json ")).hasStatus(400);
    }

    @Test
    @DisplayName("Regla de negocio - el nombre de una franquicia es unico (409)")
    void shouldRejectDuplicatedFranchiseName() {
        exchange(postJson("/api/franquicias", """
                {"nombre": "Krispy Kreme"}
                """)).hasStatus(201);

        ApiResponse response = exchange(postJson("/api/franquicias", """
                {"nombre": "krispy kreme"}
                """));

        response.hasStatus(409);
        assertThat(response.text("detail")).contains("Ya existe una franquicia");
    }

    @Test
    @DisplayName("Lectura - GET /api/franquicias lista lo creado")
    void shouldListFranchises() {
        crearFranquicia("Krispy Kreme");
        crearFranquicia("Subway");

        ApiResponse response = exchange(get("/api/franquicias"));

        response.hasStatus(200);
        assertThat(names(response.body())).containsExactly("Krispy Kreme", "Subway");
    }

    // ------------------------------------------------------------------ sucursales (CA 3)

    @Test
    @DisplayName("CA 3 - POST /api/franquicias/{id}/sucursales agrega una sucursal")
    void shouldAddBranch() {
        String franquicia = crearFranquicia("Krispy Kreme");

        ApiResponse response = exchange(postJson("/api/franquicias/" + franquicia + "/sucursales", """
                {"nombre": "Medellin centro"}
                """));

        response.hasStatus(201);
        assertThat(response.text("nombre")).isEqualTo("Medellin centro");
        assertThat(response.text("idFranquicia")).isEqualTo(franquicia);
        assertThat(response.header("Location")).contains("/sucursales/" + response.text("id"));
    }

    @Test
    @DisplayName("CA 3 - agregar una sucursal a una franquicia inexistente responde 404")
    void shouldFailAddingBranchToUnknownFranchise() {
        exchange(postJson("/api/franquicias/no-existe/sucursales", """
                {"nombre": "Medellin centro"}
                """)).hasStatus(404);
    }

    @Test
    @DisplayName("Regla de negocio - dos sucursales no pueden llamarse igual en la misma franquicia")
    void shouldRejectDuplicatedBranchNameInSameFranchise() {
        String franquicia = crearFranquicia("Krispy Kreme");
        crearSucursal(franquicia, "Centro");

        ApiResponse response = exchange(postJson("/api/franquicias/" + franquicia + "/sucursales", """
                {"nombre": "centro"}
                """));

        response.hasStatus(409);
        assertThat(response.text("detail")).contains("ya tiene una sucursal");
    }

    @Test
    @DisplayName("Regla de negocio - sucursales homonimas si se permite en franquicias distintas")
    void shouldAllowSameBranchNameInDifferentFranchises() {
        String primera = crearFranquicia("Krispy Kreme");
        String segunda = crearFranquicia("Subway");
        crearSucursal(primera, "Centro");
        crearSucursal(segunda, "Centro");

        exchange(get("/api/franquicias/" + segunda + "/sucursales"))
                .hasStatus(200);
        assertThat(exchange(get("/api/franquicias/" + segunda + "/sucursales")).body()).hasSize(1);
    }

    // ------------------------------------------------------------------ productos (CA 4)

    @Test
    @DisplayName("CA 4 - POST .../productos agrega un producto a la sucursal")
    void shouldAddProduct() {
        String franquicia = crearFranquicia("Krispy Kreme");
        String sucursal = crearSucursal(franquicia, "Centro");

        ApiResponse response = exchange(postJson(productosUri(franquicia, sucursal), """
                {"nombre": "Donas glaseadas", "stock": 25}
                """));

        response.hasStatus(201);
        assertThat(response.text("nombre")).isEqualTo("Donas glaseadas");
        assertThat(response.number("stock")).isEqualTo(25);
        assertThat(response.text("idSucursal")).isEqualTo(sucursal);
        assertThat(response.header("Location")).contains("/productos/" + response.text("id"));
    }

    @Test
    @DisplayName("CA 4 - un stock negativo responde 400")
    void shouldRejectNegativeStockOnCreate() {
        String franquicia = crearFranquicia("Krispy Kreme");
        String sucursal = crearSucursal(franquicia, "Centro");

        ApiResponse response = exchange(postJson(productosUri(franquicia, sucursal), """
                {"nombre": "Donas", "stock": -1}
                """));

        response.hasStatus(400);
        assertThat(response.body().path("errors").get(0).asText()).contains("stock");
    }

    @Test
    @DisplayName("CA 4 - un producto de una sucursal de otra franquicia no existe (404)")
    void shouldFailAddingProductToBranchOfAnotherFranchise() {
        String primera = crearFranquicia("Krispy Kreme");
        String segunda = crearFranquicia("Subway");
        String sucursalAjena = crearSucursal(segunda, "Centro");

        exchange(postJson(productosUri(primera, sucursalAjena), """
                {"nombre": "Donas", "stock": 5}
                """)).hasStatus(404);
    }

    @Test
    @DisplayName("Regla de negocio - el nombre del producto es unico dentro de la sucursal")
    void shouldRejectDuplicatedProductName() {
        String franquicia = crearFranquicia("Krispy Kreme");
        String sucursal = crearSucursal(franquicia, "Centro");
        crearProducto(franquicia, sucursal, "Donas", 10);

        ApiResponse response = exchange(postJson(productosUri(franquicia, sucursal), """
                {"nombre": "DONAS", "stock": 4}
                """));

        response.hasStatus(409);
        assertThat(response.text("detail")).contains("ya ofrece un producto");
    }

    // --------------------------------------------------------- eliminar productos (CA 5)

    @Test
    @DisplayName("CA 5 - DELETE .../productos/{id} elimina el producto y devuelve 204")
    void shouldDeleteProduct() {
        String franquicia = crearFranquicia("Krispy Kreme");
        String sucursal = crearSucursal(franquicia, "Centro");
        String producto = crearProducto(franquicia, sucursal, "Donas", 10);

        exchange(delete(productoUri(franquicia, sucursal, producto)))
                .hasStatus(204)
                .hasNoBody();

        exchange(get(productoUri(franquicia, sucursal, producto))).hasStatus(404);
    }

    @Test
    @DisplayName("CA 5 - eliminar un producto inexistente responde 404")
    void shouldFailDeletingUnknownProduct() {
        String franquicia = crearFranquicia("Krispy Kreme");
        String sucursal = crearSucursal(franquicia, "Centro");

        exchange(delete(productoUri(franquicia, sucursal, "no-existe"))).hasStatus(404);
    }

    // ------------------------------------------------------------------ stock (CA 6)

    @Test
    @DisplayName("CA 6 - PATCH .../productos/{id}/stock actualiza el stock")
    void shouldUpdateStock() {
        String franquicia = crearFranquicia("Krispy Kreme");
        String sucursal = crearSucursal(franquicia, "Centro");
        String producto = crearProducto(franquicia, sucursal, "Donas", 10);

        ApiResponse response = exchange(patchJson(stockUri(franquicia, sucursal, producto), """
                {"stock": 42}
                """));

        response.hasStatus(200);
        assertThat(response.number("stock")).isEqualTo(42);

        // el cambio queda persistido
        assertThat(exchange(get(productoUri(franquicia, sucursal, producto))).number("stock")).isEqualTo(42);
    }

    @Test
    @DisplayName("CA 6 - PUT .../stock es un alias idempotente del PATCH")
    void shouldUpdateStockWithPut() {
        String franquicia = crearFranquicia("Krispy Kreme");
        String sucursal = crearSucursal(franquicia, "Centro");
        String producto = crearProducto(franquicia, sucursal, "Donas", 10);

        exchange(putJson(stockUri(franquicia, sucursal, producto), Map.of("stock", 0)))
                .hasStatus(200)
                .numberIs("stock", 0);
    }

    @Test
    @DisplayName("CA 6 - un stock negativo en la actualizacion responde 400")
    void shouldRejectNegativeStockOnUpdate() {
        String franquicia = crearFranquicia("Krispy Kreme");
        String sucursal = crearSucursal(franquicia, "Centro");
        String producto = crearProducto(franquicia, sucursal, "Donas", 10);

        ApiResponse response = exchange(patchJson(stockUri(franquicia, sucursal, producto), """
                {"stock": -5}
                """));

        response.hasStatus(400);
        assertThat(response.body().path("errors").get(0).asText()).contains("negativo");
    }

    @Test
    @DisplayName("CA 6 - actualizar el stock de un producto inexistente responde 404")
    void shouldFailUpdatingStockOfUnknownProduct() {
        String franquicia = crearFranquicia("Krispy Kreme");
        String sucursal = crearSucursal(franquicia, "Centro");

        exchange(patchJson(stockUri(franquicia, sucursal, "no-existe"), """
                {"stock": 7}
                """)).hasStatus(404);
    }

    // ------------------------------------- producto con mas stock por sucursal (CA 7)

    @Test
    @DisplayName("CA 7 - devuelve el producto con mas stock de cada sucursal, con su sucursal")
    void shouldReportTopStockProductPerBranch() {
        String franquicia = crearFranquicia("Krispy Kreme");

        String centro = crearSucursal(franquicia, "Centro");
        crearProducto(franquicia, centro, "Donas glaseadas", 10);
        crearProducto(franquicia, centro, "Cafe", 50);
        crearProducto(franquicia, centro, "Jugo", 3);

        String norte = crearSucursal(franquicia, "Norte");
        crearProducto(franquicia, norte, "Empanada", 20);
        crearProducto(franquicia, norte, "Arepa", 5);

        String sur = crearSucursal(franquicia, "Sur");
        crearProducto(franquicia, sur, "Donut", 50);

        // Sucursal sin productos: no tiene "producto con mas stock", asi que no aparece.
        crearSucursal(franquicia, "Oeste");

        ApiResponse response = exchange(get("/api/franquicias/" + franquicia + "/productos-mayor-stock"));

        response.hasStatus(200);
        assertThat(toRows(response.body())).containsExactly(
                new String[]{"Centro", "Cafe", "50"},
                new String[]{"Sur", "Donut", "50"},
                new String[]{"Norte", "Empanada", "20"});

        // Cada entrada trae tambien los identificadores de la sucursal y del producto.
        JsonNode first = response.body().get(0);
        assertThat(first.path("idSucursal").asText()).isEqualTo(centro);
        assertThat(first.path("idProducto").asText()).isNotBlank();
    }

    @Test
    @DisplayName("CA 7 - empate de stock: se devuelven todos los productos empatados")
    void shouldReportAllTiedProducts() {
        String franquicia = crearFranquicia("Krispy Kreme");
        String sucursal = crearSucursal(franquicia, "Centro");
        crearProducto(franquicia, sucursal, "Cafe", 7);
        crearProducto(franquicia, sucursal, "Té", 7);
        crearProducto(franquicia, sucursal, "Jugo", 2);

        ApiResponse response = exchange(get("/api/franquicias/" + franquicia + "/productos-mayor-stock"));

        response.hasStatus(200);
        assertThat(toRows(response.body())).containsExactly(
                new String[]{"Centro", "Cafe", "7"},
                new String[]{"Centro", "Té", "7"});
    }

    @Test
    @DisplayName("CA 7 - una franquicia sin sucursales devuelve 200 con lista vacia")
    void shouldReturnEmptyListWhenFranchiseHasNoBranches() {
        String franquicia = crearFranquicia("Krispy Kreme");

        ApiResponse response = exchange(get("/api/franquicias/" + franquicia + "/productos-mayor-stock"));

        response.hasStatus(200);
        assertThat(response.body()).isEmpty();
    }

    @Test
    @DisplayName("CA 7 - una franquicia inexistente devuelve 404")
    void shouldFailTopStockForUnknownFranchise() {
        exchange(get("/api/franquicias/no-existe/productos-mayor-stock")).hasStatus(404);
    }

    // ---------------------------------------------------------- actualizar nombres (extra)

    @Test
    @DisplayName("Extra - PATCH /api/franquicias/{id} actualiza el nombre de la franquicia")
    void shouldRenameFranchise() {
        String franquicia = crearFranquicia("Krispy Kreme");

        exchange(patchJson("/api/franquicias/" + franquicia, """
                {"nombre": "Krispy Kreme Premium"}
                """))
                .hasStatus(200)
                .textIs("id", franquicia)
                .textIs("nombre", "Krispy Kreme Premium");

        assertThat(names(exchange(get("/api/franquicias")).body())).containsExactly("Krispy Kreme Premium");
    }

    @Test
    @DisplayName("Extra - renombrar a un nombre ya usado responde 409")
    void shouldRejectRenamingToDuplicatedName() {
        crearFranquicia("Krispy Kreme");
        String segunda = crearFranquicia("Subway");

        exchange(patchJson("/api/franquicias/" + segunda, """
                {"nombre": "Krispy Kreme"}
                """)).hasStatus(409);
    }

    @Test
    @DisplayName("Extra - renombrar una franquicia con su mismo nombre no falla")
    void shouldAllowRenamingToSameName() {
        String franquicia = crearFranquicia("Krispy Kreme");

        exchange(patchJson("/api/franquicias/" + franquicia, """
                {"nombre": "Krispy Kreme"}
                """))
                .hasStatus(200)
                .textIs("nombre", "Krispy Kreme");
    }

    @Test
    @DisplayName("Extra - PATCH .../sucursales/{id} actualiza el nombre de la sucursal")
    void shouldRenameBranch() {
        String franquicia = crearFranquicia("Krispy Kreme");
        String sucursal = crearSucursal(franquicia, "Centro");

        exchange(patchJson(sucursalUri(franquicia, sucursal), """
                {"nombre": "Centro historial"}
                """))
                .hasStatus(200)
                .textIs("nombre", "Centro historial");
    }

    @Test
    @DisplayName("Extra - PATCH .../productos/{id} actualiza el nombre del producto")
    void shouldRenameProduct() {
        String franquicia = crearFranquicia("Krispy Kreme");
        String sucursal = crearSucursal(franquicia, "Centro");
        String producto = crearProducto(franquicia, sucursal, "Donas", 10);

        exchange(patchJson(productoUri(franquicia, sucursal, producto), """
                {"nombre": "Donas de la casa"}
                """))
                .hasStatus(200)
                .textIs("nombre", "Donas de la casa")
                .numberIs("stock", 10);
    }

    @Test
    @DisplayName("Extra - renombrar un producto a un nombre ya usado en la sucursal responde 409")
    void shouldRejectRenamingProductToDuplicatedName() {
        String franquicia = crearFranquicia("Krispy Kreme");
        String sucursal = crearSucursal(franquicia, "Centro");
        crearProducto(franquicia, sucursal, "Donas", 10);
        String otro = crearProducto(franquicia, sucursal, "Cafe", 3);

        exchange(patchJson(productoUri(franquicia, sucursal, otro), """
                {"nombre": "Donas"}
                """)).hasStatus(409);
    }

    // ------------------------------------------------------------------ lecturas extra

    @Test
    @DisplayName("Lectura - GET /api/franquicias/{id} devuelve el arbol completo")
    void shouldReturnFranchiseDetail() {
        String franquicia = crearFranquicia("Krispy Kreme");
        String sucursal = crearSucursal(franquicia, "Centro");
        crearProducto(franquicia, sucursal, "Donas", 10);
        crearProducto(franquicia, sucursal, "Cafe", 50);

        ApiResponse response = exchange(get("/api/franquicias/" + franquicia));

        response.hasStatus(200)
                .textIs("id", franquicia)
                .textIs("nombre", "Krispy Kreme");
        JsonNode sucursales = response.body().path("sucursales");
        assertThat(sucursales).hasSize(1);
        assertThat(sucursales.get(0).path("nombre").asText()).isEqualTo("Centro");
        JsonNode productos = sucursales.get(0).path("productos");
        assertThat(productos).hasSize(2);
        assertThat(productos.get(0).path("nombre").asText()).isEqualTo("Cafe");
        assertThat(productos.get(0).path("stock").asInt()).isEqualTo(50);
        assertThat(productos.get(1).path("nombre").asText()).isEqualTo("Donas");
    }

    @Test
    @DisplayName("Integridad - al eliminar una sucursal se eliminan sus productos (CASCADE)")
    void shouldCascadeDeleteProductsWithBranch() {
        String franquicia = crearFranquicia("Krispy Kreme");
        String sucursal = crearSucursal(franquicia, "Centro");
        crearProducto(franquicia, sucursal, "Donas", 10);

        exchange(delete(sucursalUri(franquicia, sucursal)))
                .hasStatus(204)
                .hasNoBody();

        exchange(get(productosUri(franquicia, sucursal))).hasStatus(404);
    }

    // ------------------------------------------------------------------------- helpers

    private static String sucursalUri(String franquicia, String sucursal) {
        return "/api/franquicias/" + franquicia + "/sucursales/" + sucursal;
    }

    private static String productosUri(String franquicia, String sucursal) {
        return sucursalUri(franquicia, sucursal) + "/productos";
    }

    private static String productoUri(String franquicia, String sucursal, String producto) {
        return productosUri(franquicia, sucursal) + "/" + producto;
    }

    private static String stockUri(String franquicia, String sucursal, String producto) {
        return productoUri(franquicia, sucursal, producto) + "/stock";
    }

    /** Extrae de cada elemento del arreglo los valores de sucursal, producto y stock. */
    private static List<String[]> toRows(JsonNode array) {
        return java.util.stream.StreamSupport.stream(array.spliterator(), false)
                .map(node -> new String[]{
                        node.path("sucursal").asText(),
                        node.path("producto").asText(),
                        node.path("stock").asText()})
                .toList();
    }

    private static List<String> names(JsonNode array) {
        return java.util.stream.StreamSupport.stream(array.spliterator(), false)
                .map(node -> node.path("nombre").asText())
                .toList();
    }
}
