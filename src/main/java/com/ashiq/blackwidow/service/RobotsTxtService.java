package com.ashiq.blackwidow.service;

import com.ashiq.blackwidow.config.ScraperConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for handling robots.txt and sitemap functionality.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RobotsTxtService {

    private final JsoupService jsoupService;
    private final ScraperConfig config;

    // Cache for robots.txt rules to avoid fetching them multiple times
    private final Map<String, RobotsTxt> robotsTxtCache = new ConcurrentHashMap<>();

    // Cache for sitemaps to avoid fetching them multiple times
    private final Map<String, Set<String>> sitemapCache = new ConcurrentHashMap<>();

    // Default crawl delay in milliseconds (1 second)
    private static final long DEFAULT_CRAWL_DELAY = 1000;

    /**
     * Fetches and parses the robots.txt file for a domain.
     * 
     * @param domain The domain to fetch the robots.txt file for
     * @param scheme The scheme to use (http or https)
     * @return The parsed robots.txt file
     */
    public RobotsTxt getRobotsTxt(String domain, String scheme) {
        try {
            // Check if we already have the robots.txt file in the cache
            if (robotsTxtCache.containsKey(domain)) {
                return robotsTxtCache.get(domain);
            }

            // Construct the robots.txt URL using the provided scheme
            String robotsUrl = scheme + "://" + domain + "/robots.txt";

            log.info("Fetching robots.txt from {}", robotsUrl);

            try {
                // Fetch the robots.txt file
                Document doc = jsoupService.getContentTypeAgnosticDocument(robotsUrl);

                // Parse the robots.txt file
                RobotsTxt robotsTxt = new RobotsTxt(doc.body().text());

                // Cache the parsed robots.txt file
                robotsTxtCache.put(domain, robotsTxt);

                // Process sitemaps if any
                processSitemaps(domain, robotsTxt.getSitemaps());

                return robotsTxt;
            } catch (IOException e) {
                log.warn("Failed to fetch robots.txt from {}: {}", robotsUrl, e.getMessage());

                // If we can't fetch the robots.txt file, create an empty one (allowing all URLs)
                RobotsTxt emptyRobotsTxt = new RobotsTxt("");
                robotsTxtCache.put(domain, emptyRobotsTxt);

                return emptyRobotsTxt;
            }
        } catch (Exception e) {
            log.error("Error processing robots.txt for domain {}: {}", domain, e.getMessage());
            return new RobotsTxt(""); // Return an empty robots.txt file (allowing all URLs)
        }
    }

    /**
     * Processes sitemaps specified in a robots.txt file.
     * 
     * @param domain The domain the sitemaps belong to
     * @param sitemapUrls The URLs of the sitemaps to process
     */
    private void processSitemaps(String domain, List<String> sitemapUrls) {
        // Skip if we already have sitemaps for this domain
        if (sitemapCache.containsKey(domain)) {
            return;
        }

        Set<String> urls = new HashSet<>();

        for (String sitemapUrl : sitemapUrls) {
            try {
                log.info("Fetching sitemap from {}", sitemapUrl);

                // Fetch the sitemap
                Document doc = jsoupService.getContentTypeAgnosticDocument(sitemapUrl);

                // Parse the sitemap based on its type
                if (sitemapUrl.endsWith(".xml") || doc.toString().contains("<urlset")) {
                    // XML sitemap
                    Elements locElements = doc.select("url > loc");
                    for (Element locElement : locElements) {
                        urls.add(locElement.text());
                    }
                } else if (sitemapUrl.endsWith(".txt")) {
                    // Text sitemap
                    for (String line : doc.body().text().split("\\r?\\n")) {
                        line = line.trim();
                        if (!line.isEmpty()) {
                            urls.add(line);
                        }
                    }
                }

                log.info("Found {} URLs in sitemap {}", urls.size(), sitemapUrl);
            } catch (IOException e) {
                log.warn("Failed to fetch sitemap from {}: {}", sitemapUrl, e.getMessage());
            }
        }

        // Cache the sitemap URLs
        if (!urls.isEmpty()) {
            sitemapCache.put(domain, urls);
        }
    }

    /**
     * Gets the sitemap URLs for a domain.
     * 
     * @param domain The domain to get the sitemap URLs for
     * @return The sitemap URLs, or an empty set if none are available
     */
    public Set<String> getSitemapUrls(String domain) {
        return sitemapCache.getOrDefault(domain, new HashSet<>());
    }

    /**
     * Checks if a URL is allowed to be crawled according to the robots.txt rules.
     * 
     * @param url The URL to check
     * @param robotsTxt The robots.txt for the domain
     * @return True if the URL is allowed to be crawled, false otherwise
     */
    public boolean isAllowedByRobotsTxt(String url, RobotsTxt robotsTxt) {
        try {
            URI uri = new URI(url);

            // Get the path from the URL
            String path = uri.getPath();
            if (path.isEmpty()) {
                path = "/";
            }

            // Check if the path is allowed
            boolean allowed = robotsTxt.isAllowed(path, config.getUserAgent());

            if (!allowed) {
                log.info("URL {} is disallowed by robots.txt", url);
            }

            return allowed;
        } catch (URISyntaxException e) {
            log.error("Invalid URL format: {}", url);
            return false;
        }
    }

    /**
     * Respects the crawl delay specified in the robots.txt file.
     * 
     * @param domain The domain to respect the crawl delay for
     * @param robotsTxt The robots.txt for the domain
     */
    public void respectCrawlDelay(String domain, RobotsTxt robotsTxt) {
        try {
            // Get the crawl delay
            long crawlDelay = robotsTxt.getCrawlDelay(config.getUserAgent(), DEFAULT_CRAWL_DELAY);

            // Sleep for the crawl delay
            if (crawlDelay > 0) {
                log.debug("Respecting crawl delay of {} ms for domain {}", crawlDelay, domain);
                Thread.sleep(crawlDelay);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}