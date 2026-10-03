package dev.urlshort.link;

import java.util.Random;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * Draws short codes: {@value #LENGTH} characters from {@code [A-Za-z0-9]}, unpredictable (a
 * {@code SecureRandom} in production) and never a first path segment the service already serves
 * (business rule 1, A-23). Uniqueness is the {@code uq_link_code} constraint's job. Decision record:
 * ADR-0007.
 */
@Component
class ShortCodes {

	static final int LENGTH = 8;
	private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	private static final Set<String> RESERVED = Set.of("api", "actuator", "v3", "swagger-ui", "error");

	private final Random random;

	ShortCodes(Random random) {
		this.random = random;
	}

	// ponytail: no pre-insert uniqueness check, a collision (about 1 in 10^14 per insert) answers 500 and
	// the client retries; add an existsByCode loop if the table nears 10^9 rows or codes get shorter
	String next() {
		String code;
		do {
			StringBuilder drawn = new StringBuilder(LENGTH);
			for (int i = 0; i < LENGTH; i++) {
				drawn.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
			}
			code = drawn.toString();
		}
		while (RESERVED.contains(code));
		return code;
	}
}
