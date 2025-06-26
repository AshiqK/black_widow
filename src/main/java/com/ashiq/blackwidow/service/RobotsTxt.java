package com.ashiq.blackwidow.service;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Class to represent a parsed robots.txt file.
 */
public class RobotsTxt {
    private final Map<String, List<String>> disallowedPaths = new HashMap<>();
    private final Map<String, List<String>> allowedPaths = new HashMap<>();
    private final Map<String, Long> crawlDelays = new HashMap<>();
    
    /**
     * -- GETTER --
     *  Gets the list of sitemaps specified in the robots.txt file.
     *
     * @return The list of sitemaps
     */
    @Getter
    private final List<String> sitemaps = new ArrayList<>();

    /**
     * Parses a robots.txt file content.
     * 
     * @param content The content of the robots.txt file
     */
    public RobotsTxt(String content) {
        String currentUserAgent = "*"; // Default user agent

        for (String line : content.split("\\r?\\n")) {
            line = line.trim();

            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            // Split the line into field and value
            String[] parts = line.split(":", 2);
            if (parts.length != 2) {
                continue;
            }

            String field = parts[0].trim().toLowerCase();
            String value = parts[1].trim();

            // Process the line based on the field
            switch (field) {
                case "user-agent":
                    currentUserAgent = value.toLowerCase();
                    break;
                case "disallow":
                    if (!value.isEmpty()) {
                        disallowedPaths.computeIfAbsent(currentUserAgent, k -> new ArrayList<>()).add(value);
                    }
                    break;
                case "allow":
                    if (!value.isEmpty()) {
                        allowedPaths.computeIfAbsent(currentUserAgent, k -> new ArrayList<>()).add(value);
                    }
                    break;
                case "crawl-delay":
                    try {
                        long delay = (long) (Float.parseFloat(value) * 1000); // Convert to milliseconds
                        crawlDelays.put(currentUserAgent, delay);
                    } catch (NumberFormatException e) {
                        // Ignore invalid crawl delay
                    }
                    break;
                case "sitemap":
                    if (!value.isEmpty()) {
                        sitemaps.add(value);
                    }
                    break;
            }
        }
    }

    /**
     * Checks if a path is allowed to be crawled for a specific user agent.
     * 
     * @param path The path to check
     * @param userAgent The user agent to check for
     * @return True if the path is allowed, false otherwise
     */
    public boolean isAllowed(String path, String userAgent) {
        userAgent = userAgent.toLowerCase();

        // Check specific user agent rules first
        if (disallowedPaths.containsKey(userAgent)) {
            // Check if the path matches any allowed path
            List<String> allowed = allowedPaths.getOrDefault(userAgent, List.of());
            for (String allowPath : allowed) {
                if (pathMatches(path, allowPath)) {
                    return true;
                }
            }

            // Check if the path matches any disallowed path
            List<String> disallowed = disallowedPaths.get(userAgent);
            for (String disallowPath : disallowed) {
                if (pathMatches(path, disallowPath)) {
                    return false;
                }
            }
        }

        // If no specific rules for the user agent, check wildcard rules
        if (disallowedPaths.containsKey("*")) {
            // Check if the path matches any allowed path
            List<String> allowed = allowedPaths.getOrDefault("*", List.of());
            for (String allowPath : allowed) {
                if (pathMatches(path, allowPath)) {
                    return true;
                }
            }

            // Check if the path matches any disallowed path
            List<String> disallowed = disallowedPaths.get("*");
            for (String disallowPath : disallowed) {
                if (pathMatches(path, disallowPath)) {
                    return false;
                }
            }
        }

        // If no rules match, the path is allowed
        return true;
    }

    /**
     * Gets the crawl delay for a specific user agent.
     * 
     * @param userAgent The user agent to get the crawl delay for
     * @param defaultCrawlDelay The default crawl delay to use if not specified
     * @return The crawl delay in milliseconds, or the default crawl delay if not specified
     */
    public long getCrawlDelay(String userAgent, long defaultCrawlDelay) {
        userAgent = userAgent.toLowerCase();

        // Check specific user agent rules first
        if (crawlDelays.containsKey(userAgent)) {
            return crawlDelays.get(userAgent);
        }

        // If no specific rules for the user agent, check wildcard rules
        if (crawlDelays.containsKey("*")) {
            return crawlDelays.get("*");
        }

        // If no rules match, return the default crawl delay
        return defaultCrawlDelay;
    }

    /**
     * Checks if a path matches a robots.txt path pattern.
     * 
     * @param path The path to check
     * @param pattern The pattern to match against
     * @return True if the path matches the pattern, false otherwise
     */
    private boolean pathMatches(String path, String pattern) {
        // Convert robots.txt pattern to regex pattern
        String regexPattern = Pattern.quote(pattern)
                .replace("*", "\\E.*\\Q") // * matches any sequence of characters
                .replace("\\Q\\E", ""); // Remove empty quotes

        // Add end anchor if the pattern doesn't end with a wildcard
        if (!pattern.endsWith("*")) {
            regexPattern = "^" + regexPattern;
        }

        // Match the path against the pattern
        return Pattern.matches(regexPattern, path);
    }
}