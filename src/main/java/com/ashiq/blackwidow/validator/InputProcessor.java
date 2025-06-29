package com.ashiq.blackwidow.validator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Processor for user input, such as URLs.
 */
@Slf4j
@Component
public class InputProcessor {

    /**
     *
     *
     * @param url The URL to process
     * @return A UrlComponents object containing the validation status and any error message
     */
    public URI processUrl(String url) {

        if (url == null || url.trim().isEmpty()) {
            log.error("URL cannot be empty.");
            return null;
        }

        try {
            // Try to parse with UriComponentsBuilder
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
            return builder.build().toUri();

        } catch (Exception e) {
            log.error("URL validation failed: {}", e.getMessage());
            return null;
        }
    }
}