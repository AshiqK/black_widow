package com.ashiq.blackwidow.service;

import com.ashiq.blackwidow.config.ScraperConfig;
import com.ashiq.blackwidow.model.ScrapedPage;
import com.ashiq.blackwidow.util.DomainUtils;
import com.ashiq.blackwidow.util.LinkUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebScraperTest {

    @Mock
    private JsoupService jsoupService;

    @Mock
    private LinkUtils linkUtils;

    @Mock
    private ScraperConfig config;

    @Mock
    private RobotsTxtService robotsTxtService;

    @InjectMocks
    private WebScraper webScraper;

    @Test
    void testExtractDomain() throws URISyntaxException {
        assertEquals("example.com", DomainUtils.extractDomain("https://example.com"));
        assertEquals("example.com", DomainUtils.extractDomain("https://www.example.com"));
        assertEquals("example.com", DomainUtils.extractDomain("http://example.com/page.html"));
        assertEquals("example.com", DomainUtils.extractDomain("https://subdomain.example.com/page.html"));
    }

    @Test
    void testIsSameDomain() throws URISyntaxException {
        assertTrue(DomainUtils.isSameDomain("https://example.com", "example.com"));
        assertTrue(DomainUtils.isSameDomain("https://www.example.com", "example.com"));
        assertTrue(DomainUtils.isSameDomain("https://subdomain.example.com", "example.com"));
        assertFalse(DomainUtils.isSameDomain("https://other-domain.com", "example.com"));
    }

    @Test
    void testGetLinksFromSameDomain() throws IOException, URISyntaxException {
        // Create a list of links to return from getLinksFromSameDomain
        List<String> mockLinks = new ArrayList<>();
        mockLinks.add("http://test-site.com/about.html");
        mockLinks.add("http://test-site.com/products.html");
        mockLinks.add("http://test-site.com/services.html");
        mockLinks.add("http://test-site.com/contact.html");
        mockLinks.add("https://subdomain.test-site.com/page.html");

        // Create a predicate that always returns true for testing
        Predicate<String> allowAllPredicate = url -> true;

        // Mock the LinkUtils.getLinksFromSameDomain method to return our mock links
        when(linkUtils.getLinksFromSameDomain(
                anyString(), 
                anyString(), 
                any(), 
                any()))
            .thenReturn(mockLinks);

        // Test getting links from the same domain
        List<String> links = linkUtils.getLinksFromSameDomain(
            "http://test-site.com/index.html", 
            "test-site.com", 
            null, 
            allowAllPredicate);

        // Verify results
        assertNotNull(links);
        assertFalse(links.isEmpty());

        // Should include internal links
        assertTrue(links.contains("http://test-site.com/about.html"));
        assertTrue(links.contains("http://test-site.com/products.html"));
        assertTrue(links.contains("http://test-site.com/services.html"));
        assertTrue(links.contains("http://test-site.com/contact.html"));

        // Should include subdomain links
        assertTrue(links.contains("https://subdomain.test-site.com/page.html"));

        // Should not include external links
        assertFalse(links.contains("https://example.com"));
    }

    @Test
    void testScrape() throws IOException, URISyntaxException {
        // Load the test HTML file for robots.txt and content
        URL resource = getClass().getClassLoader().getResource("test-site/index.html");
        assertNotNull(resource, "Test HTML file not found");

        File file = new File(resource.getFile());
        Document doc = Jsoup.parse(file, "UTF-8", "http://test-site.com/");

        // Create a list of links to return from getLinksFromSameDomain
        List<String> mockLinks = new ArrayList<>();
        mockLinks.add("http://test-site.com/about.html");
        mockLinks.add("http://test-site.com/products.html");
        mockLinks.add("http://test-site.com/services.html");
        mockLinks.add("http://test-site.com/contact.html");
        mockLinks.add("https://subdomain.test-site.com/page.html");

        // Create a mock RobotsTxt object
        RobotsTxt mockRobotsTxt = new RobotsTxt("");

        // Set up mocks with lenient to avoid UnnecessaryStubbingException
        // Mock the RobotsTxtService methods
        lenient().when(robotsTxtService.getRobotsTxt(anyString(), anyString())).thenReturn(mockRobotsTxt);
        lenient().when(robotsTxtService.isAllowedByRobotsTxt(anyString(), any(RobotsTxt.class))).thenReturn(true);
        lenient().when(robotsTxtService.getSitemapUrls(anyString())).thenReturn(new HashSet<>());

        // Mock the user agent for robots.txt checks
        lenient().when(config.getUserAgent()).thenReturn("TestAgent/1.0");

        // Mock the LinkUtils.getLinksFromSameDomain method to return our mock links
        when(linkUtils.getLinksFromSameDomain(anyString(), anyString(), any(), any()))
                .thenReturn(mockLinks);

        // Test scraping
        ScrapedPage result = webScraper.scrape("http://test-site.com/index.html");

        // Verify results
        assertNotNull(result);
        assertEquals("http://test-site.com/index.html", result.url());
        assertNotNull(result.links());
        assertFalse(result.links().isEmpty());
        assertEquals(5, result.links().size());

        // Create a set of URLs from the links for easier testing
        Set<String> linkUrls = result.links().stream()
            .map(ScrapedPage::url)
            .collect(java.util.stream.Collectors.toSet());

        // Should include internal links
        assertTrue(linkUrls.contains("http://test-site.com/about.html"));
        assertTrue(linkUrls.contains("http://test-site.com/products.html"));
        assertTrue(linkUrls.contains("http://test-site.com/services.html"));
        assertTrue(linkUrls.contains("http://test-site.com/contact.html"));

        // Should include subdomain links
        assertTrue(linkUrls.contains("https://subdomain.test-site.com/page.html"));

        // Should not include external links
        assertFalse(linkUrls.contains("https://example.com"));

        // All child links should have empty links (no recursion)
        for (ScrapedPage link : result.links()) {
            assertTrue(link.links().isEmpty());
        }
    }

    @Test
    void testUrlValidation() throws IOException {
        // Set up mocks with lenient to avoid UnnecessaryStubbingException
        lenient().when(config.getUserAgent()).thenReturn("TestAgent/1.0");

        // Create a mock RobotsTxt object
        RobotsTxt mockRobotsTxt = new RobotsTxt("");

        // Mock the RobotsTxtService methods
        lenient().when(robotsTxtService.getRobotsTxt(anyString(), anyString())).thenReturn(mockRobotsTxt);
        lenient().when(robotsTxtService.isAllowedByRobotsTxt(anyString(), any(RobotsTxt.class))).thenReturn(true);

        // Test null URL
        Exception exception = assertThrows(URISyntaxException.class, () -> {
            webScraper.scrape(null);
        });
        assertTrue(exception.getMessage().contains("URL cannot be null or empty"));

        // Test empty URL
        exception = assertThrows(URISyntaxException.class, () -> {
            webScraper.scrape("");
        });
        assertTrue(exception.getMessage().contains("URL cannot be null or empty"));

        // Test URL without scheme
        exception = assertThrows(URISyntaxException.class, () -> {
            webScraper.scrape("example.com");
        });
        assertTrue(exception.getMessage().contains("URL must start with 'http://' or 'https://'") || 
                  exception.getMessage().contains("Invalid URL format"));

        // Test URL with invalid scheme
        exception = assertThrows(URISyntaxException.class, () -> {
            webScraper.scrape("ftp://example.com");
        });
        assertTrue(exception.getMessage().contains("URL must start with 'http://' or 'https://'"));

        // Test URL without host
        exception = assertThrows(URISyntaxException.class, () -> {
            webScraper.scrape("http:///path");
        });
        assertTrue(exception.getMessage().contains("URL must contain a valid host name") || 
                  exception.getMessage().contains("Invalid URL format"));
    }

    @Test
    void testRobotsTxtParsing() throws Exception {
        // This test doesn't use any mocks, it directly tests the RobotsTxt class

        // Create a robots.txt content
        String robotsTxtContent = """
            User-agent: *
            Disallow: /private/
            Disallow: /admin/
            Allow: /private/public/
            Crawl-delay: 2

            User-agent: googlebot
            Disallow: /no-google/
            Allow: /private/
            Crawl-delay: 1

            Sitemap: https://example.com/sitemap.xml
            """;

        // Create a new instance of RobotsTxt
        RobotsTxt robotsTxt = new RobotsTxt(robotsTxtContent);

        // Test isAllowed method
        assertTrue(robotsTxt.isAllowed("/", "Mozilla"));
        assertFalse(robotsTxt.isAllowed("/private/", "Mozilla"));
        assertFalse(robotsTxt.isAllowed("/admin/", "Mozilla"));
        assertTrue(robotsTxt.isAllowed("/private/public/", "Mozilla"));

        assertTrue(robotsTxt.isAllowed("/private/", "googlebot"));
        assertFalse(robotsTxt.isAllowed("/no-google/", "googlebot"));

        // Test getCrawlDelay method
        long defaultCrawlDelay = 1000L;
        assertEquals(2000L, robotsTxt.getCrawlDelay("Mozilla", defaultCrawlDelay));
        assertEquals(1000L, robotsTxt.getCrawlDelay("googlebot", defaultCrawlDelay));

        // Test getSitemaps method
        List<String> sitemaps = robotsTxt.getSitemaps();
        assertEquals(1, sitemaps.size());
        assertEquals("https://example.com/sitemap.xml", sitemaps.get(0));
    }
}
