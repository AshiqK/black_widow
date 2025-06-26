package com.ashiq.blackwidow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the web scraper.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "scraper")
public class ScraperConfig {
    
    /**
     * User agent to use for HTTP requests.
     */
    private String userAgent = "BlackWidow/1.0";
    
    /**
     * Timeout for HTTP requests in milliseconds.
     */
    private int timeout = 10000;
    
    /**
     * Whether to follow redirects.
     */
    private boolean followRedirects = true;
}