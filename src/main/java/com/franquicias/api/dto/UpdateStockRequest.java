package com.franquicias.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Payload to replace the stock of a product. */
public record UpdateStockRequest(
        @NotNull(message = "es obligatorio")
        @Min(value = 0, message = "no puede ser negativo")
        Integer stock
) {
}
