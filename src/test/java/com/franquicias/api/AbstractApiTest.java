package com.franquicias.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for the end to end API tests: boots the real application (WebFlux + R2DBC) against
 * an in memory H2 database and leaves it empty before each test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractApiTest {

    @Autowired
    protected WebTestClient client;

    @Autowired
    protected DatabaseClient databaseClient;

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        databaseClient.sql("DELETE FROM products").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM branches").fetch().rowsUpdated().block();
        databaseClient.sql("DELETE FROM franchises").fetch().rowsUpdated().block();
    }

    // ------------------------------------------------------------------ request helpers

    protected WebTestClient.RequestHeadersSpec<?> postJson(String uri, Object body) {
        return client.post().uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body instanceof String ? body : json(body));
    }

    protected WebTestClient.RequestHeadersSpec<?> patchJson(String uri, Object body) {
        return client.patch().uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body instanceof String ? body : json(body));
    }

    protected WebTestClient.RequestHeadersSpec<?> putJson(String uri, Object body) {
        return client.put().uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body instanceof String ? body : json(body));
    }

    protected WebTestClient.RequestHeadersSpec<?> delete(String uri) {
        return client.delete().uri(uri);
    }

    protected WebTestClient.RequestHeadersSpec<?> get(String uri) {
        return client.get().uri(uri);
    }

    protected WebTestClient.RequestHeadersSpec<?> postRawJson(String uri, String body) {
        return client.post().uri(uri).contentType(MediaType.APPLICATION_JSON).bodyValue(body);
    }

    // ------------------------------------------------------------------------ responses

    protected ApiResponse exchange(WebTestClient.RequestHeadersSpec<?> request) {
        EntityExchangeResult<byte[]> result = request.exchange().expectBody().returnResult();
        return new ApiResponse(result.getStatus().value(), parse(result.getResponseBody()),
                result.getResponseHeaders());
    }

    protected String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo serializar el cuerpo de prueba", ex);
        }
    }

    private JsonNode parse(byte[] body) {
        if (body == null || body.length == 0) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(body);
        } catch (IOException ex) {
            throw new IllegalStateException("La respuesta no es JSON valido", ex);
        }
    }

    /** Response under test: status, parsed body and headers. */
    protected record ApiResponse(int status, JsonNode body, HttpHeaders headers) {

        public String text(String field) {
            return body.path(field).asText();
        }

        public int number(String field) {
            return body.path(field).asInt();
        }

        public String header(String name) {
            return headers.getFirst(name);
        }

        public ApiResponse hasStatus(int expected) {
            assertThat(status).as("Codigo de estado").isEqualTo(expected);
            return this;
        }

        public ApiResponse textIs(String field, String expected) {
            assertThat(text(field)).as("campo '%s'", field).isEqualTo(expected);
            return this;
        }

        public ApiResponse numberIs(String field, int expected) {
            assertThat(number(field)).as("campo '%s'", field).isEqualTo(expected);
            return this;
        }

        public ApiResponse hasNoBody() {
            assertThat(body.isEmpty() || body.toString().equals("{}"))
                    .as("El cuerpo deberia estar vacio pero fue: %s", body)
                    .isTrue();
            return this;
        }
    }

    // ------------------------------------------------------------------ domain shortcuts

    protected String crearFranquicia(String nombre) {
        ApiResponse response = exchange(postJson("/api/franquicias", Map.of("nombre", nombre)));
        response.hasStatus(201);
        return response.text("id");
    }

    protected String crearSucursal(String franquiciaId, String nombre) {
        ApiResponse response = exchange(
                postJson("/api/franquicias/" + franquiciaId + "/sucursales", Map.of("nombre", nombre)));
        response.hasStatus(201);
        return response.text("id");
    }

    protected String crearProducto(String franquiciaId, String sucursalId, String nombre, int stock) {
        ApiResponse response = exchange(postJson(
                "/api/franquicias/" + franquiciaId + "/sucursales/" + sucursalId + "/productos",
                Map.of("nombre", nombre, "stock", stock)));
        response.hasStatus(201);
        return response.text("id");
    }
}
