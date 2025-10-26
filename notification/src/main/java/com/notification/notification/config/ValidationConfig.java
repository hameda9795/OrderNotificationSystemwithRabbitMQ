package com.notification.notification.config;

import jakarta.validation.Validator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

/**
 * Configuration for bean validation in the application.
 * Enables method-level validation and provides a configured validator.
 */
@Configuration
public class ValidationConfig {

    /**
     * Provides a validator bean for programmatic validation.
     *
     * @return Configured validator instance
     */
    @Bean
    public Validator validator() {
        return new LocalValidatorFactoryBean();
    }

    /**
     * Enables method-level validation using @Validated annotation.
     *
     * @return MethodValidationPostProcessor for AOP-based validation
     */
    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        return new MethodValidationPostProcessor();
    }
}
