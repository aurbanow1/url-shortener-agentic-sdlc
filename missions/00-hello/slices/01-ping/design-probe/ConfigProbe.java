import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.env.StandardEnvironment;

/**
 * Read-only probe of the revised functional-suite configuration mechanism
 * (profile overlay on top of the shipped application.properties). Derived
 * from docs/review/01-ping/proof/ConfigProbe.java; adds the datasource URL
 * and the active profiles so the override direction is visible.
 */
class ConfigProbe {
	public static void main(String[] args) {
		var environment = new StandardEnvironment();
		ConfigDataEnvironmentPostProcessor.applyTo(environment);
		System.out.println("activeProfiles=" + String.join(",", environment.getActiveProfiles()));
		for (String name : new String[] { "spring.application.name", "spring.mvc.problemdetails.enabled",
				"logging.structured.format.console", "spring.datasource.url", "spring.flyway.enabled" }) {
			System.out.println(name + "=" + environment.getProperty(name, "<absent>"));
		}
		environment.getPropertySources().forEach(source -> System.out.println("source=" + source.getName()));
	}
}
