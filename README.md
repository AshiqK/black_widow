# Black Widow

Black Widow is a simple web scraping application built with Spring Boot that extracts links from web pages. It finds links that belong to the same domain as the target URL and displays them in a hierarchical structure.

## What It Does

- Extracts links from web pages that belong to the same domain
- Respects robots.txt rules and sitemap.xml
- Displays results as a tree structure
- Works with both HTTP and HTTPS

## Requirements

- Java 21 or higher
- Maven 3.6 or higher

## Quick Start

1. Build the application:
   ```
   mvn clean package
   ```

2. Run the application:
   ```
   java -jar target/black-widow-0.0.1-SNAPSHOT.jar https://example.com
   ```

## Usage

```
java -jar target/black-widow-0.0.1-SNAPSHOT.jar <url>
```

Where `<url>` is the website you want to scrape (must start with http:// or https://).

### Example Output

```
Scraping https://example.com...

Results:
- https://example.com
  - https://example.com/about.html
  - https://example.com/products.html
  - https://example.com/contact.html
```

## Configuration

You can configure the application by modifying the `application.yaml` file:

```yaml
scraper:
  user-agent: BlackWidow/1.0
  timeout: 10000
  follow-redirects: true
```

## How It Works

Black Widow uses a combination of modern libraries and techniques to efficiently scrape web pages:

1. **URL Validation**: The application first validates the input URL using Spring's `UriComponentsBuilder`, ensuring it's properly formatted with a valid scheme and host.

2. **Robots.txt Handling**: Black Widow uses the industry-standard Crawler-Commons library to parse and respect robots.txt rules:
   - Fetches and parses robots.txt once per domain
   - Respects disallow directives to avoid crawling restricted areas
   - Honors crawl-delay directives to avoid overloading servers
   - Gracefully handles missing or malformed robots.txt files

3. **Link Extraction**: Using JSoup, the application:
   - Parses HTML content to extract all links
   - Filters links to include only those from the same domain
   - Checks each link against robots.txt rules
   - Adds relevant URLs from sitemaps to the results

4. **Error Handling**: The application is designed to fail gracefully:
   - Provides clear error messages for invalid URLs
   - Continues processing even if robots.txt can't be retrieved
   - Logs warnings and errors for troubleshooting

5. **Performance Optimization**:
   - Caches robots.txt and sitemap data to avoid redundant requests
   - Uses efficient data structures for storing and processing links

This architecture ensures that Black Widow is both powerful and respectful of website owners' preferences.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
