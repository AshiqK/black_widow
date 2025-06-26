package com.ashiq.blackwidow.service;

import com.ashiq.blackwidow.model.ScrapedPage;
import com.ashiq.blackwidow.util.DomainUtils;
import com.ashiq.blackwidow.util.LinkUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for scraping web pages and extracting links.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebScraper {

    private final LinkUtils linkUtils;
    private final RobotsTxtService robotsTxtService;

    /**
     * Validates the URL format.
     *
     * @param url The URL to validate
     * @throws URISyntaxException If the URL is malformed or has an invalid format
     */
    private void validateUrl(String url) throws URISyntaxException {
        if (url == null || url.trim().isEmpty()) {
            throw new URISyntaxException("", "URL cannot be null or empty. Please provide a valid URL (e.g., https://example.com)");
        }

        try {
            java.net.URI uri = new java.net.URI(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();

            if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
                throw new URISyntaxException(url, "URL must start with 'http://' or 'https://'. Please provide a valid URL (e.g., https://example.com)");
            }

            if (host == null || host.isEmpty()) {
                throw new URISyntaxException(url, "URL must contain a valid host name. Please provide a valid URL (e.g., https://example.com)");
            }
        } catch (URISyntaxException e) {
            throw new URISyntaxException(url, "Invalid URL format. Please provide a valid URL (e.g., https://example.com): " + e.getMessage());
        }
    }

    /**
     * Scrapes a web page and returns links from the same domain.
     *
     * @param url   The URL to scrape
     * @return A ScrapedPage representing the scraped page and its links
     * @throws IOException        If there's an error connecting to or parsing the URL
     * @throws URISyntaxException If the URL is malformed
     */
    public ScrapedPage scrape(String url) throws IOException, URISyntaxException {
        // Validate the URL format
        validateUrl(url);

        // Extract the domain and scheme from the URL
        String domain = DomainUtils.extractDomain(url);
        URI uri = new URI(url);
        String scheme = uri.getScheme();

        log.info("Starting scrape of {}", url);

        // Fetch robots.txt once at the start
        RobotsTxt robotsTxt = robotsTxtService.getRobotsTxt(domain, scheme);

        // Check if the URL is allowed by robots.txt
        if (!robotsTxtService.isAllowedByRobotsTxt(url, robotsTxt)) {
            log.warn("URL {} is disallowed by robots.txt. Skipping.", url);
            return new ScrapedPage(url, List.of());
        }

        // Respect crawl delay
        robotsTxtService.respectCrawlDelay(domain, robotsTxt);

        // Get all links from the page that match the domain
        List<String> links = linkUtils.getLinksFromSameDomain(
            url, 
            domain, 
            robotsTxtService.getSitemapUrls(domain), 
            linkUrl -> robotsTxtService.isAllowedByRobotsTxt(linkUrl, robotsTxt)
        );

        // Create a list of ScrapedPage objects for the links
        List<ScrapedPage> scrapedLinks = new ArrayList<>();
        for (String link : links) {
            scrapedLinks.add(new ScrapedPage(link, List.of()));
        }

        return ScrapedPage.builder().url(url).links(scrapedLinks).build();
    }
}