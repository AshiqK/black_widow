package com.ashiq.blackwidow.payload;

import lombok.Builder;

import java.util.List;

/**
 * Record representing a scraped web page and its links.
 */
@Builder
public record ScrapedPage(String url, List<ScrapedPage> links) {}