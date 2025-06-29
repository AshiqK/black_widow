package com.ashiq.blackwidow;

import com.ashiq.blackwidow.payload.ScrapedPage;
import com.ashiq.blackwidow.service.WebScraper;
import com.ashiq.blackwidow.validator.InputProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@Slf4j
@SpringBootApplication
public class BlackWidowApplication {

	public static void main(String[] args) {
		SpringApplication.run(BlackWidowApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(WebScraper webScraper, InputProcessor inputProcessor) {
		return args -> {
			String url;

			if (args.length < 1) {
				log.info("Usage: java -jar black-widow.jar <url>");
				log.info("  <url>   - The URL to scrape");
				return;
			}

			url = args[0];

			URI uri = inputProcessor.processUrl(url);
			if(uri == null) {
				log.error("There was a problem processing the URL: {}", url);
				return;
			}

			try {
				// Perform the scraping
				log.info("Scraping {}...", url);
				ScrapedPage scrapedPage = webScraper.scrape(uri);

				// Print the results
				log.info("\nResults:");
				printResults(scrapedPage, 0);

			} catch (IOException e) {
				log.error("Error connecting to or parsing the URL: {}", e.getMessage());
			}
		};
	}

	/**
	 * Iteratively prints the results tree with proper indentation.
	 *
	 * @param rootPage The root ScrapedPage to print
	 * @param rootLevel The starting indentation level
	 */
	private void printResults(ScrapedPage rootPage, int rootLevel) {
		// Create a stack to hold pages to process along with their levels
		java.util.Deque<Object[]> stack = new java.util.ArrayDeque<>();

		// Add the root page to the stack (process in reverse order for DFS)
		stack.push(new Object[]{rootPage, rootLevel});

		// Process pages until the stack is empty
		while (!stack.isEmpty()) {
			// Get the next page and its level from the stack
			Object[] item = stack.pop();
			ScrapedPage page = (ScrapedPage) item[0];
			int level = (int) item[1];

			// Print the current page URL with proper indentation
			String indent = "  ".repeat(level);
			log.info("{}- {}", indent, page.url());

			// Add child links to the stack in reverse order (to maintain original order when popped)
			List<ScrapedPage> links = page.links();
			if (!links.isEmpty()) {
				for (int i = links.size() - 1; i >= 0; i--) {
					stack.push(new Object[]{links.get(i), level + 1});
				}
			}
		}
	}
}
