package com.ashiq.blackwidow.service;

import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Adapter for Crawler-Commons' robots.txt parser.
 */
public class RobotsTxtAdapter {
    private final BaseRobotRules robotRules;

    @Getter
    private final List<String> sitemaps;

    /**
     * Creates a new RobotsTxtAdapter from robots.txt content.
     * 
     * @param content The content of the robots.txt file
     * @param userAgent The user agent to use for parsing
     */
    public RobotsTxtAdapter(String content, String userAgent) {
        SimpleRobotRulesParser parser = new SimpleRobotRulesParser();

        // Since we can't find a non-deprecated replacement for parseContent,
        // we'll continue to use it with the deprecation warning suppressed.
        // In a future update, when the correct replacement method is identified,
        // this code should be updated to use that method.
        @SuppressWarnings("deprecation")
        BaseRobotRules rules = parser.parseContent(
            "robots.txt", 
            content.getBytes(StandardCharsets.UTF_8), 
            "text/plain", 
            userAgent
        );

        this.robotRules = rules;
        this.sitemaps = robotRules.getSitemaps();
    }

    /**
     * Checks if a URL is allowed to be crawled.
     * 
     * @param url The URL to check
     * @return True if the URL is allowed, false otherwise
     */
    public boolean isAllowed(String url) {
        return robotRules.isAllowed(url);
    }

    /**
     * Gets the crawl delay in milliseconds.
     * 
     * @return The crawl delay in milliseconds
     */
    public long getCrawlDelay() {
        return robotRules.getCrawlDelay() * 1000; // Convert to milliseconds
    }
}
