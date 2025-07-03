package com.skillnez.cloudstorage.dto;

import com.skillnez.cloudstorage.utils.PasswordMatchValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordMatchValidator.class)
public @interface PasswordMatches {
    String message() default "Password does not match";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
