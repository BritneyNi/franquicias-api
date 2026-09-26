package com.franquicias.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** A business rule was violated, e.g. a duplicated name. Rendered as {@code 409}. */
public class ConflictException extends ResponseStatusException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }

    public static ConflictException duplicateFranchise(String name) {
        return new ConflictException("Ya existe una franquicia con el nombre \"" + name + "\"");
    }

    public static ConflictException duplicateBranch(String name) {
        return new ConflictException("La franquicia ya tiene una sucursal con el nombre \"" + name + "\"");
    }

    public static ConflictException duplicateProduct(String name) {
        return new ConflictException("La sucursal ya ofrece un producto con el nombre \"" + name + "\"");
    }
}
