package com.hackathon.energia.validation;

import com.hackathon.energia.exception.InvalidEnumValueException;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnumValidValidatorTest {

    private EnumValidValidator validator;
    private EnumValid annotation;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new EnumValidValidator();
        annotation = mock(EnumValid.class);
        context = mock(ConstraintValidatorContext.class);

        when(annotation.values()).thenReturn(new String[]{"Casa", "Apartamento", "Local"});
        validator.initialize(annotation);
    }

    @Nested
    @DisplayName("Valores válidos")
    class ValoresValidos {

        @Test
        @DisplayName("'Casa' → retorna true")
        void testValorValidoCasa() {
            assertTrue(validator.isValid("Casa", context));
        }

        @Test
        @DisplayName("'Apartamento' → retorna true")
        void testValorValidoApartamento() {
            assertTrue(validator.isValid("Apartamento", context));
        }

        @Test
        @DisplayName("'Local' → retorna true")
        void testValorValidoLocal() {
            assertTrue(validator.isValid("Local", context));
        }
    }

    @Nested
    @DisplayName("Valores inválidos")
    class ValoresInvalidos {

        @Test
        @DisplayName("'Oficina' → lanza InvalidEnumValueException")
        void testValorInvalidoLanzaExcepcion() {
            assertThrows(InvalidEnumValueException.class, () ->
                    validator.isValid("Oficina", context)
            );
        }

        @Test
        @DisplayName("'casa' (minúscula) → lanza InvalidEnumValueException (case-sensitive)")
        void testValorInvalidoCaseSensitive() {
            assertThrows(InvalidEnumValueException.class, () ->
                    validator.isValid("casa", context)
            );
        }

        @Test
        @DisplayName("'Local Comercial' → lanza InvalidEnumValueException")
        void testValorInvalidoCompuesto() {
            assertThrows(InvalidEnumValueException.class, () ->
                    validator.isValid("Local Comercial", context)
            );
        }
    }

    @Nested
    @DisplayName("Valores null o blank")
    class ValoresNullBlank {

        @Test
        @DisplayName("null → retorna true (validación de @NotBlank separada)")
        void testValorNullPasa() {
            assertTrue(validator.isValid(null, context));
        }

        @Test
        @DisplayName("'' (blank) → retorna true (validación de @NotBlank separada)")
        void testValorBlankPasa() {
            assertTrue(validator.isValid("", context));
        }

        @Test
        @DisplayName("'   ' (solo espacios) → retorna true (validación de @NotBlank separada)")
        void testValorSoloEspaciosPasa() {
            assertTrue(validator.isValid("   ", context));
        }
    }

    @Nested
    @DisplayName("Mensaje de excepción")
    class MensajeExcepcion {

        @Test
        @DisplayName("La excepción contiene el valor inválido y la lista de permitidos")
        void testMensajeContieneInfo() {
            var ex = assertThrows(InvalidEnumValueException.class, () ->
                    validator.isValid("Oficina", context)
            );

            assertTrue(ex.getMessage().contains("Oficina"));
            assertTrue(ex.getMessage().contains("no está en la lista de valores permitidos"));
        }
    }
}
