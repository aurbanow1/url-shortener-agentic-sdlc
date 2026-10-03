package dev.urlshort.link;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

import org.junit.jupiter.api.Test;

/** Business rule 1: code shape, unpredictability source and the reserved-segment re-draw. */
class ShortCodesTest {

	private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

	@Test
	void codesAreEightCharactersFromTheAlphanumericAlphabet() {
		ShortCodes codes = new ShortCodes(new SecureRandom());

		String first = codes.next();
		String second = codes.next();

		assertThat(first).matches("[A-Za-z0-9]{8}");
		assertThat(second).matches("[A-Za-z0-9]{8}").isNotEqualTo(first);
	}

	@Test
	void aDrawThatSpellsAReservedSegmentIsDrawnAgain() {
		ShortCodes codes = new ShortCodes(scripted("actuator", "Abc12345"));

		assertThat(codes.next()).isEqualTo("Abc12345");
	}

	/** A {@link Random} whose {@code nextInt(bound)} spells the given codes in turn. */
	private static Random scripted(String... spelled) {
		Deque<Integer> indices = new ArrayDeque<>();
		for (String code : spelled) {
			code.chars().forEach(c -> indices.add(ALPHABET.indexOf(c)));
		}
		return new Random() {
			@Override
			public int nextInt(int bound) {
				return indices.removeFirst();
			}
		};
	}
}
