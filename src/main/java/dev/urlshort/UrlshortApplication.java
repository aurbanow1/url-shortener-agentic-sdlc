package dev.urlshort;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Boot entry point of the urlshort service: component scanning and auto-configuration start from
 * this package, so every feature package ({@code link}, {@code audit}, {@code web}, {@code ping})
 * lives below it.
 */
@SpringBootApplication
public class UrlshortApplication {

	/** Instantiated by Spring as the root configuration class; holds no state. */
	public UrlshortApplication() {
	}

	/**
	 * Starts the service with the shipped configuration ({@code application.properties}, overridable
	 * by environment variables and command-line arguments).
	 *
	 * @param args Spring Boot command-line arguments, for example {@code --server.port=8081}
	 */
	public static void main(String[] args) {
		SpringApplication.run(UrlshortApplication.class, args);
	}
}
