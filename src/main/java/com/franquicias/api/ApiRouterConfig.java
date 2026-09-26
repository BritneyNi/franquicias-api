package com.franquicias.api;

import com.franquicias.api.dto.ApiMapper.BranchResponse;
import com.franquicias.api.dto.ApiMapper.FranchiseDetailResponse;
import com.franquicias.api.dto.ApiMapper.FranchiseResponse;
import com.franquicias.api.dto.ApiMapper.ProductResponse;
import com.franquicias.api.dto.ApiMapper.TopStockProductResponse;
import com.franquicias.api.dto.CreateBranchRequest;
import com.franquicias.api.dto.CreateFranchiseRequest;
import com.franquicias.api.dto.CreateProductRequest;
import com.franquicias.api.dto.RenameRequest;
import com.franquicias.api.dto.UpdateStockRequest;
import com.franquicias.exception.InvalidRequestException;
import com.franquicias.service.BranchService;
import com.franquicias.service.FranchiseService;
import com.franquicias.service.ProductService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Every HTTP endpoint of the API, declared as a <a
 * href="https://docs.spring.io/spring-framework/reference/web/webflux-webfunc/functional-endpoints.html">
 * functional route</a> (composed {@link RouterFunction} instead of annotated controllers).
 *
 * <p>Handlers are plain functions from {@link ServerRequest} to {@code Mono<ServerResponse>}, and
 * every write delegates to a service returning a {@code Mono}/{@code Flux}: no thread is blocked
 * anywhere in the request path.
 */
@Configuration(proxyBeanMethods = false)
public class ApiRouterConfig {

    private static final String FRANQUICIAS = "/api/franquicias";
    private static final String FRANQUICIA = FRANQUICIAS + "/{franquiciaId}";
    private static final String SUCURSALES = FRANQUICIA + "/sucursales";
    private static final String SUCURSAL = SUCURSALES + "/{sucursalId}";
    private static final String PRODUCTOS = SUCURSAL + "/productos";
    private static final String PRODUCTO = PRODUCTOS + "/{productoId}";
    private static final String STOCK = PRODUCTO + "/stock";

    @Bean
    public RouterFunction<ServerResponse> apiRouter(FranchiseService franchises,
                                                    BranchService branches,
                                                    ProductService products,
                                                    RequestValidator validator) {

        Function<ServerRequest, Mono<CreateFranchiseRequest>> createFranchiseBody = body(validator, CreateFranchiseRequest.class);
        Function<ServerRequest, Mono<CreateBranchRequest>> createBranchBody = body(validator, CreateBranchRequest.class);
        Function<ServerRequest, Mono<CreateProductRequest>> createProductBody = body(validator, CreateProductRequest.class);
        Function<ServerRequest, Mono<UpdateStockRequest>> updateStockBody = body(validator, UpdateStockRequest.class);
        Function<ServerRequest, Mono<RenameRequest>> renameBody = body(validator, RenameRequest.class);

        return RouterFunctions.route()
                // ---------------------------------------------------------------- franquicias
                .POST(FRANQUICIAS, request -> createFranchiseBody.apply(request)
                        .flatMap(body -> franchises.create(body.nombre()))
                        .map(FranchiseResponse::from)
                        .flatMap(franchise -> created(
                                URI.create(FRANQUICIAS + "/" + franchise.id()), franchise)))

                .GET(FRANQUICIAS, request -> franchises.findAll()
                        .map(FranchiseResponse::from)
                        .collectList()
                        .flatMap(ApiRouterConfig::ok))

                .GET(FRANQUICIA, request -> branches.detail(franquiciaId(request))
                        .map(FranchiseDetailResponse::from)
                        .flatMap(ApiRouterConfig::ok))

                .PATCH(FRANQUICIA, request -> renameBody.apply(request)
                        .flatMap(body -> franchises.rename(franquiciaId(request), body.nombre()))
                        .map(FranchiseResponse::from)
                        .flatMap(ApiRouterConfig::ok))

                // ---------------------------------------------------------------- sucursales
                .POST(SUCURSALES, request -> createBranchBody.apply(request)
                        .flatMap(body -> branches.add(franquiciaId(request), body.nombre()))
                        .map(BranchResponse::from)
                        .flatMap(branch -> created(
                                URI.create(FRANQUICIAS + "/" + branch.idFranquicia()
                                        + "/sucursales/" + branch.id()), branch)))

                .GET(SUCURSALES, request -> branches.findByFranchiseId(franquiciaId(request))
                        .map(BranchResponse::from)
                        .collectList()
                        .flatMap(ApiRouterConfig::ok))

                .GET(SUCURSAL, request -> branches.findById(franquiciaId(request), sucursalId(request))
                        .map(BranchResponse::from)
                        .flatMap(ApiRouterConfig::ok))

                .PATCH(SUCURSAL, request -> renameBody.apply(request)
                        .flatMap(body -> branches.rename(franquiciaId(request), sucursalId(request), body.nombre()))
                        .map(BranchResponse::from)
                        .flatMap(ApiRouterConfig::ok))

                .DELETE(SUCURSAL, request -> branches.delete(franquiciaId(request), sucursalId(request))
                        .then(ServerResponse.noContent().build()))

                // ---------------------------------------------------------------- productos
                .POST(PRODUCTOS, request -> createProductBody.apply(request)
                        .flatMap(body -> products.add(franquiciaId(request), sucursalId(request),
                                body.nombre(), body.stock()))
                        .map(ProductResponse::from)
                        .flatMap(product -> created(
                                URI.create(productoLocation(franquiciaId(request), sucursalId(request), product.id())),
                                product)))

                .GET(PRODUCTOS, request -> products.findByBranch(franquiciaId(request), sucursalId(request))
                        .map(ProductResponse::from)
                        .collectList()
                        .flatMap(ApiRouterConfig::ok))

                .GET(PRODUCTO, request -> products.findById(franquiciaId(request), sucursalId(request), productoId(request))
                        .map(ProductResponse::from)
                        .flatMap(ApiRouterConfig::ok))

                .PATCH(PRODUCTO, request -> renameBody.apply(request)
                        .flatMap(body -> products.rename(franquiciaId(request), sucursalId(request),
                                productoId(request), body.nombre()))
                        .map(ProductResponse::from)
                        .flatMap(ApiRouterConfig::ok))

                .DELETE(PRODUCTO, request -> products.remove(franquiciaId(request), sucursalId(request), productoId(request))
                        .then(ServerResponse.noContent().build()))

                .PATCH(STOCK, request -> updateStockBody.apply(request)
                        .flatMap(body -> products.updateStock(franquiciaId(request), sucursalId(request),
                                productoId(request), body.stock()))
                        .map(ProductResponse::from)
                        .flatMap(ApiRouterConfig::ok))

                // Idempotent alias: PUT /stock has exactly the same semantics as PATCH /stock.
                .PUT(STOCK, request -> updateStockBody.apply(request)
                        .flatMap(body -> products.updateStock(franquiciaId(request), sucursalId(request),
                                productoId(request), body.stock()))
                        .map(ProductResponse::from)
                        .flatMap(ApiRouterConfig::ok))

                // ------------------------------------------------- producto con mas stock
                .GET(FRANQUICIA + "/productos-mayor-stock", request -> products
                        .topStockPerBranch(franquiciaId(request))
                        .map(TopStockProductResponse::from)
                        .collectList()
                        .flatMap(ApiRouterConfig::ok))

                // ----------------------------------------------------------------- index
                .GET("/api", request -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(index()))

                .build();
    }

    private Map<String, Object> index() {
        return Map.of(
                "nombre", "franquicias-api",
                "version", "1.0.0",
                "documentacion", "README.md",
                "franquicias", Map.of(
                        "crear", "POST " + FRANQUICIAS,
                        "listar", "GET " + FRANQUICIAS,
                        "detalle", "GET " + FRANQUICIA,
                        "renombrar", "PATCH " + FRANQUICIA,
                        "productoConMasStock", "GET " + FRANQUICIA + "/productos-mayor-stock"),
                "sucursales", Map.of(
                        "crear", "POST " + SUCURSALES,
                        "listar", "GET " + SUCURSALES,
                        "renombrar", "PATCH " + SUCURSAL,
                        "eliminar", "DELETE " + SUCURSAL),
                "productos", Map.of(
                        "crear", "POST " + PRODUCTOS,
                        "listar", "GET " + PRODUCTOS,
                        "renombrar", "PATCH " + PRODUCTO,
                        "eliminar", "DELETE " + PRODUCTO,
                        "actualizarStock", "PATCH " + STOCK));
    }

    /**
     * Reads the JSON body, validates it and never lets an empty body slip through: an absent
     * payload is a {@code 400}, not an empty {@code 200}.
     */
    private <T> Function<ServerRequest, Mono<T>> body(RequestValidator validator, Class<T> type) {
        return request -> request.bodyToMono(type)
                .map(validator::validate)
                .switchIfEmpty(Mono.error(() -> new InvalidRequestException(
                        "El cuerpo de la petición es obligatorio",
                        List.of("Se esperaba un cuerpo JSON con los campos requeridos"))));
    }

    private Mono<ServerResponse> created(URI location, Object body) {
        return ServerResponse.created(location).bodyValue(body);
    }

    /** Shorthand for {@code ServerResponse.ok().bodyValue(body)} used all over the routes. */
    private static Mono<ServerResponse> ok(Object body) {
        return ServerResponse.ok().bodyValue(body);
    }

    private static String productoLocation(String franchiseId, String branchId, String productId) {
        return "/api/franquicias/" + franchiseId + "/sucursales/" + branchId + "/productos/" + productId;
    }

    private String franquiciaId(ServerRequest request) {
        return request.pathVariable("franquiciaId");
    }

    private String sucursalId(ServerRequest request) {
        return request.pathVariable("sucursalId");
    }

    private String productoId(ServerRequest request) {
        return request.pathVariable("productoId");
    }
}
