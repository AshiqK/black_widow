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
import java.util.Set;

/**
 * Service for handling robots.txt and sitemap functionality.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Getter
public class RobotsTxtService {

    private final JsoupService jsoupService;
    private final ScraperConfig config;

    private RobotsTxtAdapter robotsTxt;
    private Set<String> sitemap;

    /**
     * Initializes the robots.txt service for a specific domain and scheme.
     * This method should be called before any other methods to ensure the robots.txt file is fetched only once.
     * 
     * @param domain The domain to initialize for
     * @param uri the URI of the target
     * @return True if initialization was successful, false otherwise
     */
    public boolean initialize(URI uri, String domain) {


        try {
            // Check if we already have the robots.txt file in the cache
            if (robotsTxt != null) {
                return true;
            }

            // Construct the robots.txt URL using the provided scheme
            String robotsUrl = uri.getScheme() + "://" + domain + "/robots.txt";

            log.info("Initializing robots.txt from {}", robotsUrl);

            try {
                // Fetch the robots.txt file, preserving original formatting
                String content = jsoupService.getRawContent(robotsUrl);

                // Log the content for debugging
                log.debug("Robots.txt content: {}", content);

                // Parse with Crawler-Commons
                robotsTxt = new RobotsTxtAdapter(content, config.getUserAgent());

                // Process sitemaps
                processSitemaps(robotsTxt.getSitemaps());
                return true;
            } catch (IOException e) {
                log.warn("Initialization Failed ",e);
                return false;
            }
        } catch (Exception e) {
            log.error("Unexpected error during robots initialization",e);
            return false;
        }
    }


    /**
     * Processes sitemaps specified in a robots.txt file.
     * 
     * @param sitemapUrls The URLs of the sitemaps to process
     */
    private void processSitemaps(List<String> sitemapUrls) {
        // Skip if we already have sitemaps for this domain
        if (sitemap != null) {
            return;
        }

        Set<String> urls = new HashSet<>();
        SiteMapParser siteMapParser = new SiteMapParser();

        for (String sitemapUrl : sitemapUrls) {
            try {
                log.info("Fetching sitemap from {}", sitemapUrl);

                // Fetch the sitemap, preserving original formatting
                byte[] content = jsoupService.getRawContent(sitemapUrl)
                    .getBytes(StandardCharsets.UTF_8);

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
        sitemap = urls;
        log.info("Found {} URLs in {} sitemaps", urls.size(), sitemapUrls.size());
    }

    /**
     * Checks if a URL is allowed to be crawled according to the robots.txt rules.
     * If robots.txt is malformed or couldn't be retrieved, all URLs are allowed.
     * 
     * @param url The URL to check
     * @return True if the URL is allowed to be crawled, false otherwise
     */
    public boolean isAllowed(String url) {
        try {
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
     */
    public void respectCrawlDelay() {

        try {
            long crawlDelay;
            crawlDelay = robotsTxt.getCrawlDelay();
            log.debug("Respecting crawl delay of {} ms ", crawlDelay);
            Thread.sleep(crawlDelay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
