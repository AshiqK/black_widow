package com.ashiq.blackwidow.integration;

import com.ashiq.blackwidow.model.ScrapedPage;
import com.ashiq.blackwidow.service.JsoupService;
import com.ashiq.blackwidow.service.RobotsTxtService;
import com.ashiq.blackwidow.service.WebScraper;
import com.ashiq.blackwidow.validator.InputValidator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the WebScraper component.
 * These tests exercise the main scraping functionality against the real site <a href="https://sedna.com/">...</a>.
 */
@Slf4j
@SpringBootTest
public class WebScraperIntegrationTests {

    private static final String TEST_URL = "https://sedna.com/";
    private static final String DOMAIN = "sedna.com";

    @Autowired
    private WebScraper webScraper;

    @Autowired
    private RobotsTxtService robotsTxtService;

    @Autowired
    private JsoupService jsoupService;

    @Autowired
    private com.ashiq.blackwidow.config.ScraperConfig scraperConfig;

    @Autowired
    private com.ashiq.blackwidow.util.LinkUtils linkUtils;

    @Autowired
    private InputValidator inputValidator;

    /**
     * Tests that the scraper correctly scrapes the homepage and returns links from the same domain.
     */
    @Test
    public void testScrapeHomepage() throws IOException {
        // Execute the scrape
        ScrapedPage scrapedPage = webScraper.scrape(TEST_URL);

        // Log the results
        log.info("[DEBUG_LOG] Scraped {} links from {}", scrapedPage.links().size(), TEST_URL);

        // Assert that we got some links
        assertFalse(scrapedPage.links().isEmpty(), "Should have found links on the homepage");

        // Extract all URLs from the scraped page
        List<String> urls = scrapedPage.links().stream()
                .map(ScrapedPage::url)
                .toList();

        // Log some of the found URLs
        urls.stream().limit(5).forEach(url -> log.info("[DEBUG_LOG] Found URL: {}", url));

        // Assert that all URLs are from the sedna.com domain
        for (String url : urls) {
            assertTrue(url.contains(DOMAIN), 
                    "URL should be from the sedna.com domain: " + url);
        }
    }
}
