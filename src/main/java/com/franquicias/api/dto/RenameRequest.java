package com.franquicias.api.dto;

import com.franquicias.support.Names;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload used by the three rename endpoints (franquicia, sucursal and producto). */
public record RenameRequest(
        @NotBlank(message = "es obligatorio")
        @Size(max = Names.MAX_LENGTH, message = "no puede superar 120 caracteres")
        String nombre
) {
}
