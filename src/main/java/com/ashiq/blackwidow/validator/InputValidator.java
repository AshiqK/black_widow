package com.ashiq.blackwidow.validator;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Validator for user input, such as URLs.
 */
@Slf4j
@Component
public class InputValidator {

    /**
     * Validates a URL using UriComponentsBuilder.
     *
     * @param url The URL to validate
     * @return A ValidationResult object containing the validation status and any error message
     */
    public ValidationResult validateUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return ValidationResult.invalid("URL cannot be null or empty. Please provide a valid URL (e.g., https://example.com)");
        }

        try {
            // Try to parse with UriComponentsBuilder
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
            URI uri = builder.build().toUri();
            
            // Check scheme
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
                return ValidationResult.invalid("URL must start with 'http://' or 'https://'. Please provide a valid URL (e.g., https://example.com)");
            }
            
            // Check host
            String host = uri.getHost();
            if (host == null || host.isEmpty()) {
                return ValidationResult.invalid("URL must contain a valid host name. Please provide a valid URL (e.g., https://example.com)");
            }
            
            return ValidationResult.valid();
        } catch (Exception e) {
            log.debug("URL validation failed: {}", e.getMessage());
            return ValidationResult.invalid("Invalid URL format. Please provide a valid URL (e.g., https://example.com): " + e.getMessage());
        }
    }

    /**
     * Class representing the result of a validation.
     */
    @Getter
    public static class ValidationResult {
        /**
         * -- GETTER --
         *  Checks if the validation result is valid.
         *
         * @return True if the validation result is valid, false otherwise
         */
        private final boolean valid;
        /**
         * -- GETTER --
         *  Gets the error message.
         *
         * @return The error message, or null if the validation result is valid
         */
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        /**
         * Creates a valid validation result.
         *
         * @return A valid validation result
         */
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        /**
         * Creates an invalid validation result with an error message.
         *
         * @param errorMessage The error message
         * @return An invalid validation result
         */
        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

    }
}