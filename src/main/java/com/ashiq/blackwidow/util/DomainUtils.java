package com.ashiq.blackwidow.util;

import com.google.common.net.InternetDomainName;
import java.net.URI;
import java.net.URISyntaxException;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for domain-related operations.
 */
@Slf4j
public class DomainUtils {

    /**
     * Extracts the domain from a URL using Guava's InternetDomainName.
     * This provides more robust domain extraction, handling cases like co.uk, com.au, etc.
     *
     * @param uri URI to extract the domain from
     * @return The domain of the URL
     */
    public static String extractDomain(URI uri) {
        String host = uri.getHost();

        if (host == null) {
            log.debug("No host found in URI: {}", uri);
            return "";
        }

        try {
            // Use Guava's InternetDomainName to extract the top private domain
            InternetDomainName internetDomainName = InternetDomainName.from(host);

            // Check if the domain has a public suffix
            if (internetDomainName.hasPublicSuffix()) {
                // Get the top private domain (e.g., example.com from www.example.com or subdomain.example.com)
                // This also handles special cases like co.uk, com.au, etc.
                return internetDomainName.topPrivateDomain().toString();
            } else {
                // If there's no public suffix, return the original host without www
                return host.startsWith("www.") ? host.substring(4) : host;
            }
        } catch (IllegalArgumentException e) {
            // This can happen with IP addresses or invalid domain names
            log.debug("Could not parse domain using InternetDomainName: {}, error: {}", host, e.getMessage());

            // Remove "www." prefix if present as a fallback
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }

            return host;
        }
    }

    /**
     * Checks if a URL is from the same domain as the specified domain.
     * Uses the improved domain extraction with Guava's InternetDomainName.
     *
     * @param uri    The URL to check
     * @param domain The domain to compare against
     * @return True if the URL is from the same domain, false otherwise
     * @throws URISyntaxException If the URL is malformed
     */
    public static boolean isSameDomain(URI uri, String domain) throws URISyntaxException {
        String urlDomain = extractDomain(uri);
        return domain.equalsIgnoreCase(urlDomain);
    }
}