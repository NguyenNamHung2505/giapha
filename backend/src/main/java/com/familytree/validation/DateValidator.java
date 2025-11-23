package com.familytree.validation;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.*;
import java.time.LocalDate;

/**
 * Validator to ensure death date is after birth date
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateValidator.DateRangeValidator.class)
@Documented
public @interface DateValidator {

    String message() default "Death date must be after birth date";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class DateRangeValidator implements ConstraintValidator<DateValidator, Object> {

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            }

            try {
                // Use reflection to get birthDate and deathDate fields
                LocalDate birthDate = (LocalDate) value.getClass().getMethod("getBirthDate").invoke(value);
                LocalDate deathDate = (LocalDate) value.getClass().getMethod("getDeathDate").invoke(value);

                if (birthDate == null || deathDate == null) {
                    return true; // One or both dates are null, can't validate
                }

                return !deathDate.isBefore(birthDate);
            } catch (Exception e) {
                return true; // If we can't access the fields, skip validation
            }
        }
    }
}
