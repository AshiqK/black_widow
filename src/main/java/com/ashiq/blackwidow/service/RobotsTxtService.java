package com.ashiq.blackwidow.service;

import com.ashiq.blackwidow.config.ScraperConfig;
import crawlercommons.sitemaps.AbstractSiteMap;
import crawlercommons.sitemaps.SiteMap;
import crawlercommons.sitemaps.SiteMapIndex;
import crawlercommons.sitemaps.SiteMapParser;
import crawlercommons.sitemaps.SiteMapURL;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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

    // Cache for robots.txt rules
    private final Map<String, RobotsTxtAdapter> robotsTxtCache = new ConcurrentHashMap<>();

    // Cache for sitemaps
    private final Map<String, Set<String>> sitemapCache = new ConcurrentHashMap<>();

    // Default crawl delay in milliseconds
    private static final long DEFAULT_CRAWL_DELAY = 1000;

    // Flag to track if robots.txt is malformed or couldn't be retrieved
    @Getter
    private boolean robotsTxtMalformed = false;

    // Flag to track if robots.txt has been initialized
    private boolean initialized = false;

    // Current domain and scheme
    private String currentDomain;
    private String currentScheme;

    /**
     * Initializes the robots.txt service for a specific domain and scheme.
     * This method should be called before any other methods to ensure the robots.txt file is fetched only once.
     * 
     * @param domain The domain to initialize for
     * @param scheme The scheme to use (http or https)
     * @return True if initialization was successful, false otherwise
     */
    public boolean initialize(String domain, String scheme) {
        // Reset the malformed flag
        robotsTxtMalformed = false;

        // Store the current domain and scheme
        this.currentDomain = domain;
        this.currentScheme = scheme;

        try {
            // Check if we already have the robots.txt file in the cache
            if (robotsTxtCache.containsKey(domain)) {
                initialized = true;
                return true;
            }

            // Construct the robots.txt URL using the provided scheme
            String robotsUrl = scheme + "://" + domain + "/robots.txt";

            log.info("Initializing robots.txt from {}", robotsUrl);

            try {
                // Fetch the robots.txt file
                String content = jsoupService.getContentTypeAgnosticDocument(robotsUrl).body().text();

                // Parse with Crawler-Commons
                RobotsTxtAdapter adapter = new RobotsTxtAdapter(content, config.getUserAgent());
                robotsTxtCache.put(domain, adapter);

                // Process sitemaps
                processSitemaps(domain, adapter.getSitemaps());

                initialized = true;
                return true;
            } catch (IOException e) {
                log.warn("Failed to fetch robots.txt from {}: {}", robotsUrl, e.getMessage());

                // Create empty rules if robots.txt can't be fetched
                RobotsTxtAdapter emptyAdapter = new RobotsTxtAdapter("", config.getUserAgent());
                robotsTxtCache.put(domain, emptyAdapter);

                // Mark as malformed but still initialized
                robotsTxtMalformed = true;
                initialized = true;
                return false;
            }
        } catch (Exception e) {
            log.error("Error processing robots.txt for domain {}: {}", domain, e.getMessage());

            // Create empty rules if robots.txt can't be processed
            robotsTxtCache.put(domain, new RobotsTxtAdapter("", config.getUserAgent()));

            // Mark as malformed but still initialized
            robotsTxtMalformed = true;
            initialized = true;
            return false;
        }
    }

    /**
     * Fetches and parses the robots.txt file for a domain.
     * This method is protected and should only be used internally or for testing.
     * 
     * @param domain The domain to fetch the robots.txt file for
     * @param scheme The scheme to use (http or https)
     * @return The parsed robots.txt file
     */
    protected RobotsTxtAdapter getRobotsTxt(String domain, String scheme) {
        // If not initialized or different domain/scheme, initialize first
        if (!initialized || !domain.equals(currentDomain) || !scheme.equals(currentScheme)) {
            initialize(domain, scheme);
        }

        // Return the cached robots.txt
        return robotsTxtCache.getOrDefault(domain, new RobotsTxtAdapter("", config.getUserAgent()));
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
        SiteMapParser siteMapParser = new SiteMapParser();

        for (String sitemapUrl : sitemapUrls) {
            try {
                log.info("Fetching sitemap from {}", sitemapUrl);

                // Fetch the sitemap
                byte[] content = jsoupService.getContentTypeAgnosticDocument(sitemapUrl)
                    .toString().getBytes(StandardCharsets.UTF_8);

                // Parse with Crawler-Commons
                URI uri = new URI(sitemapUrl);
                URL url = uri.toURL();
                AbstractSiteMap abstractSiteMap = siteMapParser.parseSiteMap(content, url);

                // Handle different types of sitemaps
                if (abstractSiteMap.isIndex()) {
                    // It's a sitemap index, process each sitemap in the index
                    SiteMapIndex siteMapIndex = (SiteMapIndex) abstractSiteMap;
                    for (AbstractSiteMap subSiteMap : siteMapIndex.getSitemaps()) {
                        if (subSiteMap instanceof SiteMap actualSiteMap) {
                            for (SiteMapURL siteMapUrl : actualSiteMap.getSiteMapUrls()) {
                                urls.add(siteMapUrl.getUrl().toString());
                            }
                        }
                    }
                } else if (abstractSiteMap instanceof SiteMap siteMap) {
                    // It's a regular sitemap
                    for (SiteMapURL siteMapUrl : siteMap.getSiteMapUrls()) {
                        urls.add(siteMapUrl.getUrl().toString());
                    }
                }

                log.info("Found {} URLs in sitemap {}", urls.size(), sitemapUrl);
            } catch (Exception e) {
                log.warn("Failed to fetch/parse sitemap from {}: {}", sitemapUrl, e.getMessage());
            }
        }

        // Cache the results
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
     * If robots.txt is malformed or couldn't be retrieved, all URLs are allowed.
     * 
     * @param url The URL to check
     * @param domain The domain of the URL
     * @param scheme The scheme of the URL (http or https)
     * @return True if the URL is allowed to be crawled, false otherwise
     */
    public boolean isAllowed(String url, String domain, String scheme) {
        // If not initialized or different domain/scheme, initialize first
        if (!initialized || !domain.equals(currentDomain) || !scheme.equals(currentScheme)) {
            initialize(domain, scheme);
        }

        // If robots.txt is malformed, allow all URLs
        if (robotsTxtMalformed) {
            log.debug("Robots.txt is malformed or couldn't be retrieved, allowing URL: {}", url);
            return true;
        }

        try {
            RobotsTxtAdapter robotsTxt = getRobotsTxt(domain, scheme);
            return robotsTxt.isAllowed(url);
        } catch (Exception e) {
            log.error("Error checking if URL {} is allowed: {}", url, e.getMessage());
            // If there's an error, allow the URL to be crawled
            return true;
        }
    }

    /**
     * Respects the crawl delay specified in the robots.txt file.
     * If robots.txt is malformed or couldn't be retrieved, uses the default crawl delay.
     * 
     * @param domain The domain to respect the crawl delay for
     * @param scheme The scheme of the URL (http or https)
     */
    public void respectCrawlDelay(String domain, String scheme) {
        // If not initialized or different domain/scheme, initialize first
        if (!initialized || !domain.equals(currentDomain) || !scheme.equals(currentScheme)) {
            initialize(domain, scheme);
        }

        try {
            long crawlDelay = DEFAULT_CRAWL_DELAY;

            // If robots.txt is not malformed, get the crawl delay from it
            if (!robotsTxtMalformed) {
                RobotsTxtAdapter robotsTxt = getRobotsTxt(domain, scheme);
                crawlDelay = robotsTxt.getCrawlDelay();
                if (crawlDelay <= 0) {
                    crawlDelay = DEFAULT_CRAWL_DELAY;
                }
            } else {
                log.debug("Robots.txt is malformed or couldn't be retrieved, using default crawl delay");
            }

            log.debug("Respecting crawl delay of {} ms for domain {}", crawlDelay, domain);
            Thread.sleep(crawlDelay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
