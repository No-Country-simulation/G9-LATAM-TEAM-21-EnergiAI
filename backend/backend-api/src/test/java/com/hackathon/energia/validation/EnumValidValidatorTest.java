package com.hackathon.energia.validation;

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
        when(annotation.message()).thenReturn("tipo_inmueble debe ser Casa, Apartamento o Local");
        when(context.buildConstraintViolationWithTemplate(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(mock(ConstraintValidatorContext.ConstraintViolationBuilder.class));
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
        @DisplayName("'Oficina' → retorna false")
        void testValorInvalidoRetornaFalse() {
            assertFalse(validator.isValid("Oficina", context));
        }

        @Test
        @DisplayName("'casa' (minúscula) → retorna false (case-sensitive)")
        void testValorInvalidoCaseSensitive() {
            assertFalse(validator.isValid("casa", context));
        }

        @Test
        @DisplayName("'Local Comercial' → retorna false")
        void testValorInvalidoCompuesto() {
            assertFalse(validator.isValid("Local Comercial", context));
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
    @DisplayName("Constraint violation")
    class ConstraintViolation {

        @Test
        @DisplayName("Valor inválido → deshabilita mensaje por defecto y agrega violation con mensaje custom")
        void testMensajeCustom() {
            assertFalse(validator.isValid("Oficina", context));
            org.mockito.Mockito.verify(context).disableDefaultConstraintViolation();
            org.mockito.Mockito.verify(context).buildConstraintViolationWithTemplate("tipo_inmueble debe ser Casa, Apartamento o Local");
        }
    }
}
