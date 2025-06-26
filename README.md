# Black Widow

Black Widow is a web scraping application that extracts links from web pages. It focuses on finding links that belong to the same domain as the target URL and displays them in a hierarchical structure.

## Features

- Extract all links from a web page that belong to the same domain
- Display results as a hierarchical tree structure
- Filter out external links automatically
- Handle subdomains correctly
- Respect robots.txt rules (disallow directives, crawl-delay)
- Utilize sitemap.xml for additional URL discovery
- Validate URLs before processing
- Support for both HTTP and HTTPS protocols

## Requirements

- Java 21 or higher
- Maven 3.6 or higher

## Installation

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/black-widow.git
   cd black-widow
   ```

2. Build the application with Maven:
   ```
   mvn clean package
   ```

This will create a JAR file in the `target` directory.

## Usage

Run the application using the following command:

```
java -jar target/black-widow-0.0.1-SNAPSHOT.jar <url>
```

### Parameters:

- `<url>` (required): The URL to scrape (e.g., https://example.com)
  - Must be a valid URL starting with http:// or https://
  - Must contain a valid host name

### Examples:

1. Scrape a page with HTTP:
   ```
   java -jar target/black-widow-0.0.1-SNAPSHOT.jar http://example.com
   ```

2. Scrape a page with HTTPS:
   ```
   java -jar target/black-widow-0.0.1-SNAPSHOT.jar https://example.com
   ```

3. Scrape a specific page:
   ```
   java -jar target/black-widow-0.0.1-SNAPSHOT.jar https://example.com/about.html
   ```

### Output:

The application outputs a tree structure showing all the links found during scraping:

```
Scraping https://example.com...

Results:
- https://example.com
  - https://example.com/about.html
  - https://example.com/products.html
  - https://example.com/contact.html
  - https://example.com/blog.html
  - https://example.com/services.html
```

## How It Works

Black Widow uses JSoup to parse HTML content and extract links. For each page:

1. Validates the URL format to ensure it's properly formed
2. Extracts the domain and scheme (HTTP/HTTPS) from the URL
3. Checks the robots.txt file to ensure the URL is allowed to be crawled
4. Respects the crawl-delay directive specified in robots.txt
5. Connects to the URL and downloads the HTML content
6. Parses the HTML to extract all links (`<a href>` tags)
7. Filters the links to include only those from the same domain and allowed by robots.txt
8. If available, uses sitemap.xml to discover additional URLs
9. Builds a hierarchical tree of all links discovered

The application handles various edge cases:
- Properly extracts domains from URLs with subdomains
- Handles relative and absolute URLs correctly
- Caches robots.txt and sitemap data to avoid redundant requests
- Gracefully handles 403 Forbidden errors and other HTTP errors
- Supports both HTTP and HTTPS protocols

## Project Structure

- `BlackWidowApplication.java`: Main application entry point and command-line interface
- `WebScraper.java`: Core service that handles the web scraping logic
- `RobotsTxtService.java`: Service for handling robots.txt parsing and caching
- `JsoupService.java`: Service for centralizing all Jsoup operations
- `ScraperConfig.java`: Configuration properties for the web scraper
- `LinkUtils.java`: Utility for extracting links from web pages
- `DomainUtils.java`: Utility for domain-related operations
- `RobotsTxt.java`: Model for representing a parsed robots.txt file
- `ScrapedPage.java`: Model for representing a scraped web page and its links
- Test resources: Sample HTML files for testing the scraper

## License

This project is licensed under the MIT License - see the LICENSE file for details.
