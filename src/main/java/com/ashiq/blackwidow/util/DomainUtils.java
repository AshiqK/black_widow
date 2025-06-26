package com.ashiq.blackwidow.util;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Utility class for domain-related operations.
 */
public class DomainUtils {

    /**
     * Extracts the domain from a URL.
     *
     * @param url The URL to extract the domain from
     * @return The domain of the URL
     * @throws URISyntaxException If the URL is malformed
     */
    public static String extractDomain(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String host = uri.getHost();

        if (host == null) {
            return "";
        }

        // Remove "www." prefix if present
        if (host.startsWith("www.")) {
            host = host.substring(4);
        }

        // Extract the base domain (e.g., example.com from subdomain.example.com)
        String[] parts = host.split("\\.");
        if (parts.length > 2) {
            // Get the last two parts (e.g., example.com)
            return parts[parts.length - 2] + "." + parts[parts.length - 1];
        }

        return host;
    }

    /**
     * Checks if a URL is from the same domain as the specified domain.
     *
     * @param url    The URL to check
     * @param domain The domain to compare against
     * @return True if the URL is from the same domain, false otherwise
     * @throws URISyntaxException If the URL is malformed
     */
    public static boolean isSameDomain(String url, String domain) throws URISyntaxException {
        String urlDomain = extractDomain(url);
        return domain.equalsIgnoreCase(urlDomain);
    }
}