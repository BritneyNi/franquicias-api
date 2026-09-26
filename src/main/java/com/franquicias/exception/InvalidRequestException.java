package com.franquicias.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * The payload is syntactically valid but breaks a validation rule. Rendered as {@code 400} with
 * the list of violated fields.
 *
 * @param errors field level messages, always non empty
 */
public class InvalidRequestException extends ResponseStatusException {

    private final transient List<String> errors;

    public InvalidRequestException(String message, List<String> errors) {
        super(HttpStatus.BAD_REQUEST, message);
        this.errors = List.copyOf(errors);
    }

    public InvalidRequestException(String message) {
        this(message, List.of(message));
    }

    public List<String> getErrors() {
        return errors;
    }
}
