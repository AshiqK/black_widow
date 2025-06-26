package com.ashiq.blackwidow.model;

import lombok.Builder;

import java.util.List;

/**
 * Record representing a scraped web page and its links.
 */
@Builder
public record ScrapedPage(String url, List<ScrapedPage> links) {
}