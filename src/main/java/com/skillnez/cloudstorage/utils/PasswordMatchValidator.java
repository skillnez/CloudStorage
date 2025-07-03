package com.skillnez.cloudstorage.utils;

import com.skillnez.cloudstorage.dto.PasswordMatches;
import com.skillnez.cloudstorage.dto.UserRegistrationRequestDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.annotation.Annotation;

public class PasswordMatchValidator implements ConstraintValidator<PasswordMatches, UserRegistrationRequestDto> {

    @Override
    public boolean isValid(UserRegistrationRequestDto user, ConstraintValidatorContext constraintValidatorContext) {
        return  user.getPassword() != null && !user.getPassword().equals(user.getRepeatPassword());
    }
}
