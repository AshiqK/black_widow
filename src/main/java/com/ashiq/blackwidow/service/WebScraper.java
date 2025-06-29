package com.ashiq.blackwidow.service;

import com.ashiq.blackwidow.payload.ScrapedPage;
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
     * Scrapes a web page and returns links from the same domain.
     *
     * @param uri   The URI to scrape
     * @return A ScrapedPage representing the scraped page and its links
     * @throws IOException If there's an error connecting to or parsing the URL
     */
    public ScrapedPage scrape(URI uri) throws IOException, URISyntaxException {

        String domain = DomainUtils.extractDomain(uri);

        log.info("Starting scrape of {}", uri.toURL());

        // Initialize robots.txt service for this domain
        boolean robotsTxtInitialized = robotsTxtService.initialize(uri,domain);
        if (!robotsTxtInitialized) {
            log.warn("Robots.txt could not be properly initialized for {}. Will proceed with scraping but some URLs might be disallowed by the site owner.", domain);
        }

        // Check if the URL is allowed by robots.txt
        if (!robotsTxtService.isAllowed(uri.toURL().toString())) {
            log.warn("URL {} is disallowed by robots.txt. Skipping.", uri.toURL());
            return new ScrapedPage(uri.toURL().toString(), List.of());
        }

        // Respect crawl delay
        robotsTxtService.respectCrawlDelay();

        // Get all links from the page that match the domain
        List<String> links;
        try {
            links = linkUtils.getLinksFromSameDomain(
                uri.toURL().toString(),
                domain,
                robotsTxtService.getSitemap(),
                    robotsTxtService::isAllowed
            );
        } catch (URISyntaxException e) {
            log.error("Failed to extract links: {}", e.getMessage());
            return new ScrapedPage(uri.toURL().toString(), List.of());
        }

        // Create a list of ScrapedPage objects for the links
        List<ScrapedPage> scrapedLinks = new ArrayList<>();
        for (String link : links) {
            scrapedLinks.add(new ScrapedPage(link, List.of()));
        }

        return ScrapedPage.builder().url(uri.toURL().toString()).links(scrapedLinks).build();
    }
}
