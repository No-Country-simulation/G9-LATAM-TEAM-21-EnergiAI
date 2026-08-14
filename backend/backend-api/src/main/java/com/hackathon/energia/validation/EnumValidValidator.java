package com.hackathon.energia.validation;

import com.hackathon.energia.exception.InvalidEnumValueException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class EnumValidValidator implements ConstraintValidator<EnumValid, String> {

    private Set<String> valoresPermitidos;
    private String fieldName;

    @Override
    public void initialize(EnumValid constraintAnnotation) {
        valoresPermitidos = Arrays.stream(constraintAnnotation.values()).collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        if (!valoresPermitidos.contains(value)) {
            throw new InvalidEnumValueException(
                    String.format("Valor '%s' no está en la lista de valores permitidos: %s",
                            value, String.join(", ", valoresPermitidos))
            );
        }
        return true;
    }
}
