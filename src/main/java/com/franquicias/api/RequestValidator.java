package com.franquicias.api;

import com.franquicias.exception.InvalidRequestException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Runs Bean Validation on the request records and turns the violations into a single
 * {@link InvalidRequestException} carrying every field message.
 */
@Component
public class RequestValidator {

    private final Validator validator;

    public RequestValidator(Validator validator) {
        this.validator = validator;
    }

    /**
     * @param payload request record to validate, may be {@code null}
     * @return the very same payload when it is valid
     * @throws InvalidRequestException when the payload is {@code null} or breaks a constraint
     */
    public <T> T validate(T payload) {
        if (payload == null) {
            throw new InvalidRequestException("El cuerpo de la petición es obligatorio",
                    List.of("Se esperaba un cuerpo JSON con el campo 'nombre'"));
        }
        Set<ConstraintViolation<T>> violations = validator.validate(payload);
        if (violations.isEmpty()) {
            return payload;
        }
        List<String> errors = violations.stream()
                .map(violation -> "%s: %s".formatted(violation.getPropertyPath(), violation.getMessage()))
                .sorted()
                .toList();
        throw new InvalidRequestException("La petición no cumple las reglas de validación", errors);
    }
}
