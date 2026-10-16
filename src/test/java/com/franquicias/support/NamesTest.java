package com.franquicias.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests of the pure name rules: no Spring context, no database, no mocks. */
class NamesTest {

    @Test
    @DisplayName("normalize recorta y colapsa los espacios internos")
    void shouldNormalizeWhitespace() {
        assertThat(Names.normalize("  Burger   King  ")).isEqualTo("Burger King");
        assertThat(Names.normalize("Krispy\tKreme")).isEqualTo("Krispy Kreme");
        assertThat(Names.normalize(null)).isNull();
    }

    @ParameterizedTest
    @DisplayName("key ignora mayusculas, acentos y espacios")
    @CsvSource({
            "Krispy Kreme, krispy kreme",
            "  CAFE  , cafe",
            "Panadería, panaderia",
            "Jugo de mango, jugo de mango"
    })
    void shouldBuildComparisonKey(String input, String expected) {
        assertThat(Names.key(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("normalizeStrict rechaza nombres vacios o demasiado largos")
    void shouldRejectInvalidNames() {
        assertThatThrownBy(() -> Names.normalizeStrict("la franquicia", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obligatorio");

        assertThatThrownBy(() -> Names.normalizeStrict("la sucursal", null))
                .isInstanceOf(IllegalArgumentException.class);

        String tooLong = "a".repeat(Names.MAX_LENGTH + 1);
        assertThatThrownBy(() -> Names.normalizeStrict("el producto", tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("120");
    }

    @Test
    @DisplayName("keyWith compone la normalizacion con otra funcion")
    void shouldComposeWithAnotherNormalizer() {
        var upper = Names.keyWith(value -> Names.normalize(value) + "!");
        assertThat(upper.apply("  KFC ")).isEqualTo("kfc!");
    }
}
