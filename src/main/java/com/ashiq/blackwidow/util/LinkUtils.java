package com.ashiq.blackwidow.util;

import com.ashiq.blackwidow.service.JsoupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Component for link-related operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LinkUtils {

    private final JsoupService jsoupService;

    /**
     * Gets all links from a web page that are from the same domain.
     *
     * @param url    The URL to scrape
     * @param domain The domain to filter links by
     * @param sitemapUrls The URLs from the sitemap
     * @param isAllowedByRobotsTxt Predicate to check if a URL is allowed by robots.txt
     * @return A list of links from the same domain
     * @throws IOException        If there's an error connecting to or parsing the URL
     * @throws URISyntaxException If the URL is malformed
     */
    public List<String> getLinksFromSameDomain(String url, String domain, Set<String> sitemapUrls, Predicate<String> isAllowedByRobotsTxt) throws IOException, URISyntaxException {
        List<String> result = new ArrayList<>();

        // Connect to the URL and get the HTML document
        Document doc = jsoupService.getDocument(url);

        // Extract all links
        Elements links = doc.select("a[href]");

        // Filter links to only include those from the same domain
        for (Element link : links) {
            String href = link.attr("abs:href").trim();

            // Skip empty links
            if (href.isEmpty()) {
                continue;
            }

            try{

                URI hrefUri = new URI(href);
                // Check if the link is from the same domain
                if (DomainUtils.isSameDomain(hrefUri, domain)) {
                    // Check if the link is allowed by robots.txt
                    if (isAllowedByRobotsTxt.test(href)) {
                        result.add(href);
                    } else {
                        log.debug("Link {} is disallowed by robots.txt. Skipping.", href);
                    }
                }

            } catch (URISyntaxException e) {
                log.warn("Skipping Invalid URL in link: {}", href);
            }


        }

        // Add sitemap URLs if available
        if (sitemapUrls != null && !sitemapUrls.isEmpty()) {
            for (String sitemapUrl : sitemapUrls) {
                try {
                    URI sitemapUri = new URI(sitemapUrl);
                    if (DomainUtils.isSameDomain(sitemapUri, domain) && !result.contains(sitemapUrl)) {
                        // Check if the sitemap URL is allowed by robots.txt
                        if (isAllowedByRobotsTxt.test(sitemapUrl)) {
                            result.add(sitemapUrl);
                        } else {
                            log.debug("Sitemap URL {} is disallowed by robots.txt. Skipping.", sitemapUrl);
                        }
                    }
                } catch (URISyntaxException e) {
                    log.warn("Skipping Invalid URL in sitemap: {}", sitemapUrl);
                }
            }
        }

        return result;
    }
}
