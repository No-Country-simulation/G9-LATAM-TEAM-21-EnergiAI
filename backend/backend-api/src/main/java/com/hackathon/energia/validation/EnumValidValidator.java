package com.hackathon.energia.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class EnumValidValidator implements ConstraintValidator<EnumValid, String> {

    private Set<String> valoresPermitidos;
    private String message;

    @Override
    public void initialize(EnumValid constraintAnnotation) {
        valoresPermitidos = Arrays.stream(constraintAnnotation.values()).collect(Collectors.toSet());
        message = constraintAnnotation.message();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        if (!valoresPermitidos.contains(value)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(message)
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}
