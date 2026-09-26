package com.franquicias.api.dto;

import com.franquicias.support.Names;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload to create a branch: {@code POST /api/franquicias/{id}/sucursales}. */
public record CreateBranchRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = Names.MAX_LENGTH, message = "no puede superar 120 caracteres")
        String nombre
) {
}
