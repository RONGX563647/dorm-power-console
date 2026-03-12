package com.dormpower.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PeriodValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PeriodValid {
    String message() default "period is invalid, must be one of: 7d, 30d";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
