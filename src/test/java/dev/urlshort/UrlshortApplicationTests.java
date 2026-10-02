package dev.urlshort;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UrlshortApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void mainBootsWithoutAWebServer() {
		assertThatCode(() -> UrlshortApplication.main(new String[] {
				"--spring.main.web-application-type=none", "--spring.main.banner-mode=off" }))
				.doesNotThrowAnyException();
	}
}
