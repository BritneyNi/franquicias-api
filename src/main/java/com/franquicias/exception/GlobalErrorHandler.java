package com.franquicias.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.spi.R2dbcDataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Single place where every error becomes an HTTP response, for both the functional routes and
 * the framework level failures (unknown path, wrong method, unreadable body...).
 *
 * <p>Bodies follow <a href="https://www.rfc-editor.org/rfc/rfc9457">RFC 9457</a>
 * ({@code application/problem+json}) so clients get a predictable error contract:
 *
 * <pre>{@code
 * {
 *   "type": "https://franquicias-api.dev/errors/409",
 *   "title": "Conflicto",
 *   "status": 409,
 *   "detail": "Ya existe una franquicia con el nombre \"Krispy Kreme\"",
 *   "instance": "/api/franquicias",
 *   "timestamp": "2026-01-01T00:00:00Z"
 * }
 * }</pre>
 */
@Component
@Order(-2)
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);
    private static final String ERROR_TYPE_BASE = "https://franquicias-api.dev/errors/";

    private final ObjectMapper objectMapper;

    public GlobalErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ProblemDetail problem = toProblemDetail(ex);
        ServerHttpRequest request = exchange.getRequest();
        problem.setInstance(URI.create(request.getPath().value()));
        if (problem.getStatus() >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            log.error("Error no controlado en {} {}", request.getMethod(), request.getPath(), ex);
        }
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatusCode.valueOf(problem.getStatus()));
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        byte[] payload = serialize(new ApiError(problem, Instant.now()));
        return response.writeWith(Mono.fromSupplier(() -> response.bufferFactory().wrap(payload)));
    }

    private byte[] serialize(ApiError error) {
        try {
            return objectMapper.writeValueAsBytes(error);
        } catch (JsonProcessingException e) {
            log.error("No se pudo serializar la respuesta de error", e);
            return ("{\"status\":500,\"title\":\"Error interno\"}").getBytes(StandardCharsets.UTF_8);
        }
    }

    private ProblemDetail toProblemDetail(Throwable ex) {
        for (Throwable cause : causeChain(ex)) {
            ProblemDetail problem = match(cause);
            if (problem != null) {
                return problem;
            }
        }
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrió un error inesperado al procesar la petición", Map.of());
    }

    private static List<Throwable> causeChain(Throwable ex) {
        List<Throwable> chain = new ArrayList<>();
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable current = ex; current != null && visited.add(current); current = current.getCause()) {
            chain.add(current);
        }
        return chain;
    }

    private ProblemDetail match(Throwable cause) {
        if (cause instanceof InvalidRequestException invalid) {
            return problem(HttpStatus.BAD_REQUEST, "Solicitud inválida", invalid.getReason(),
                    Map.of("errors", invalid.getErrors()));
        }
        if (cause instanceof UnsupportedMediaTypeStatusException) {
            return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de medio no soportado",
                    "El Content-Type debe ser " + MediaType.APPLICATION_JSON_VALUE, Map.of());
        }
        if (cause instanceof MethodNotAllowedException notAllowed) {
            return problem(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido",
                    "Método no soportado en este recurso: " + notAllowed.getMessage(), Map.of());
        }
        if (cause instanceof DataBufferLimitException) {
            return problem(HttpStatus.PAYLOAD_TOO_LARGE, "Cuerpo demasiado grande",
                    "El cuerpo de la petición excede el tamaño máximo permitido", Map.of());
        }
        if (cause instanceof ServerWebInputException) {
            return problem(HttpStatus.BAD_REQUEST, "Cuerpo de la petición inválido",
                    "El JSON enviado no se pudo procesar: " + cause.getMessage(), Map.of());
        }
        if (cause instanceof R2dbcDataIntegrityViolationException) {
            // Unique/check constraint violated: a concurrent request won the race, so the
            // pre-check performed by the service was not enough.
            return problem(HttpStatus.CONFLICT, "Conflicto de datos",
                    "La operación viola una restricción de integridad de datos", Map.of());
        }
        if (cause instanceof DataIntegrityViolationException integrity) {
            return problem(HttpStatus.CONFLICT, "Conflicto de datos",
                    "La operación viola una restricción de integridad de datos: " + integrity.getMessage(),
                    Map.of());
        }
        if (cause instanceof ResponseStatusException statusException) {
            HttpStatus resolved = HttpStatus.resolve(statusException.getStatusCode().value());
            HttpStatus status = resolved == null ? HttpStatus.INTERNAL_SERVER_ERROR : resolved;
            String detail = statusException.getReason() == null
                    ? "La petición no pudo completarse"
                    : statusException.getReason();
            return problem(status, title(status), detail, Map.of());
        }
        return null;
    }

    /**
     * The domain exceptions ({@code NotFoundException}, {@code ConflictException}) and the
     * framework failures ({@code NoResourceFoundException}, ...) all arrive wrapped in a
     * {@code ResponseStatusException}, so the title is resolved here to keep the error contract in
     * Spanish instead of using the English reason phrase of the status.
     */
    private static String title(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Solicitud inválida";
            case UNAUTHORIZED -> "No autenticado";
            case FORBIDDEN -> "Acceso denegado";
            case NOT_FOUND -> "Recurso no encontrado";
            case METHOD_NOT_ALLOWED -> "Método no permitido";
            case CONFLICT -> "Conflicto";
            case UNSUPPORTED_MEDIA_TYPE -> "Tipo de medio no soportado";
            case PAYLOAD_TOO_LARGE -> "Cuerpo demasiado grande";
            case UNPROCESSABLE_ENTITY -> "Entidad no procesable";
            case INTERNAL_SERVER_ERROR -> "Error interno";
            case SERVICE_UNAVAILABLE -> "Servicio no disponible";
            default -> status.getReasonPhrase();
        };
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, Map<String, Object> properties) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(ERROR_TYPE_BASE + status.value()));
        properties.forEach(problem::setProperty);
        return problem;
    }

    /** Convenience factory used by the router and the tests. */
    public static InvalidRequestException invalid(String message, List<String> errors) {
        return new InvalidRequestException(message, errors);
    }

    /**
     * Envelope written to the client: the RFC 9457 problem plus the extra members that make API
     * consumption easier.
     *
     * @param errors field level messages, only present when the problem carries them
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ApiError(
            String type,
            String title,
            int status,
            String detail,
            String instance,
            Instant timestamp,
            @JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> errors
    ) {
        ApiError(ProblemDetail problem, Instant timestamp) {
            this(
                    problem.getType() == null ? null : problem.getType().toString(),
                    problem.getTitle(),
                    problem.getStatus(),
                    problem.getDetail(),
                    problem.getInstance() == null ? null : problem.getInstance().toString(),
                    timestamp,
                    fieldErrors(problem));
        }

        /** Unwraps the {@code errors} problem extension, ignoring anything of another shape. */
        private static List<String> fieldErrors(ProblemDetail problem) {
            Map<String, Object> properties = problem.getProperties();
            if (properties == null) {
                return List.of();
            }
            Object errors = properties.get("errors");
            if (errors instanceof List<?> messages) {
                return messages.stream().map(String::valueOf).toList();
            }
            return List.of();
        }
    }
}
