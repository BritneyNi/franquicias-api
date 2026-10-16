package com.franquicias.api.dto;

import com.franquicias.domain.Franchise;
import com.franquicias.support.Names;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload of {@code POST /api/franquicias}. */
public record CreateFranchiseRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = Names.MAX_LENGTH, message = "no puede superar 120 caracteres")
        String nombre
) {
}
