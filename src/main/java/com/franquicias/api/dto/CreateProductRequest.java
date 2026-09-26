package com.franquicias.api.dto;

import com.franquicias.support.Names;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Payload to create a product: {@code POST /api/franquicias/{f}/sucursales/{s}/productos}. */
public record CreateProductRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = Names.MAX_LENGTH, message = "no puede superar 120 caracteres")
        String nombre,

        @NotNull(message = "es obligatorio")
        @Min(value = 0, message = "no puede ser negativo")
        Integer stock
) {
}
