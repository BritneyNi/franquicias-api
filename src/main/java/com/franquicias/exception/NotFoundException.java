package com.franquicias.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** The requested franchise, branch or product does not exist. Rendered as {@code 404}. */
public class NotFoundException extends ResponseStatusException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    public static NotFoundException franchise(String id) {
        return new NotFoundException("No existe la franquicia con id " + id);
    }

    public static NotFoundException branch(String franchiseId, String branchId) {
        return new NotFoundException(
                "No existe la sucursal con id " + branchId + " en la franquicia " + franchiseId);
    }

    public static NotFoundException product(String branchId, String productId) {
        return new NotFoundException(
                "No existe el producto con id " + productId + " en la sucursal " + branchId);
    }
}
