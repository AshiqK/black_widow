package com.ashiq.blackwidow.service;

import com.ashiq.blackwidow.model.ScrapedPage;
import com.ashiq.blackwidow.util.DomainUtils;
import com.ashiq.blackwidow.util.LinkUtils;
import com.ashiq.blackwidow.validator.InputValidator;
import com.ashiq.blackwidow.validator.InputValidator.ValidationResult;
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
    private final InputValidator inputValidator;


    /**
     * Scrapes a web page and returns links from the same domain.
     *
     * @param url   The URL to scrape
     * @return A ScrapedPage representing the scraped page and its links
     * @throws IOException If there's an error connecting to or parsing the URL
     */
    public ScrapedPage scrape(String url) throws IOException {
        // Validate the URL format
        ValidationResult validationResult = inputValidator.validateUrl(url);
        if (!validationResult.isValid()) {
            log.error("URL validation failed: {}", validationResult.getErrorMessage());
            return new ScrapedPage(url, List.of());
        }

        String domain;
        String scheme;

        try {
            // Extract the domain and scheme from the URL
            domain = DomainUtils.extractDomain(url);
            URI uri = new URI(url);
            scheme = uri.getScheme();
        } catch (URISyntaxException e) {
            log.error("Failed to parse URL: {}", e.getMessage());
            return new ScrapedPage(url, List.of());
        }

        log.info("Starting scrape of {}", url);

        // Initialize robots.txt service for this domain
        boolean robotsTxtInitialized = robotsTxtService.initialize(domain, scheme);
        if (!robotsTxtInitialized) {
            log.warn("Robots.txt could not be properly initialized for {}. Will proceed with scraping but some URLs might be disallowed by the site owner.", domain);
        }

        // Check if the URL is allowed by robots.txt
        if (!robotsTxtService.isAllowed(url, domain, scheme)) {
            log.warn("URL {} is disallowed by robots.txt. Skipping.", url);
            return new ScrapedPage(url, List.of());
        }

        // Respect crawl delay
        robotsTxtService.respectCrawlDelay(domain, scheme);

        // Get all links from the page that match the domain
        List<String> links;
        try {
            links = linkUtils.getLinksFromSameDomain(
                url, 
                domain, 
                robotsTxtService.getSitemapUrls(domain), 
                linkUrl -> robotsTxtService.isAllowed(linkUrl, domain, scheme)
            );
        } catch (URISyntaxException e) {
            log.error("Failed to extract links: {}", e.getMessage());
            return new ScrapedPage(url, List.of());
        }

        // Create a list of ScrapedPage objects for the links
        List<ScrapedPage> scrapedLinks = new ArrayList<>();
        for (String link : links) {
            scrapedLinks.add(new ScrapedPage(link, List.of()));
        }

        return ScrapedPage.builder().url(url).links(scrapedLinks).build();
    }
}
