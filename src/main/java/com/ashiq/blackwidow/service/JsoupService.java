package com.ashiq.blackwidow.service;

import com.ashiq.blackwidow.config.ScraperConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Service for handling all Jsoup operations.
 * This centralizes all Jsoup usage to ensure consistent configuration and behavior.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JsoupService {

    private final ScraperConfig config;

    /**
     * Gets an HTML document from a URL.
     *
     * @param url The URL to get the document from
     * @return The HTML document
     * @throws IOException If there's an error connecting to or parsing the URL
     */
    public Document getDocument(String url) throws IOException {
        try {
            return Jsoup.connect(url)
                    .userAgent(config.getUserAgent())
                    .timeout(config.getTimeout())
                    .followRedirects(config.isFollowRedirects())
                    .get();
        } catch (org.jsoup.HttpStatusException e) {
            // Handle HTTP errors, particularly 403 Forbidden
            if (e.getStatusCode() == 403) {
                log.warn("Received HTTP 403 Forbidden when accessing URL: {}. This website may be blocking web scrapers.", url);
                log.warn("Consider using a real browser or adding authentication if required.");

                // Return an empty document with a warning message
                return Jsoup.parse("<html><body><p>Error: This website returned HTTP 403 Forbidden. It may be blocking web scrapers.</p></body></html>");
            }
            // Re-throw other HTTP errors
            throw e;
        } catch (IOException e) {
            log.error("Error connecting to URL: {}", url, e);
            throw e;
        }
    }

    /**
     * Gets a document that may not be HTML (like robots.txt or sitemap.xml).
     *
     * @param url The URL to get the document from
     * @return The document
     * @throws IOException If there's an error connecting to or parsing the URL
     */
    public Document getContentTypeAgnosticDocument(String url) throws IOException {
        try {
            return Jsoup.connect(url)
                    .userAgent(config.getUserAgent())
                    .timeout(config.getTimeout())
                    .followRedirects(config.isFollowRedirects())
                    .ignoreContentType(true)
                    .get();
        } catch (IOException e) {
            log.error("Error connecting to URL: {}", url, e);
            throw e;
        }
    }
}