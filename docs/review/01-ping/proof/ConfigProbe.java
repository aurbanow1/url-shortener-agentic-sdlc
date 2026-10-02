import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.env.StandardEnvironment;

/** Read-only probe of the design's configuration-loading assumption. */
class ConfigProbe {
    public static void main(String[] args) {
        var environment = new StandardEnvironment();
        ConfigDataEnvironmentPostProcessor.applyTo(environment);
        for (String name : new String[] {"spring.application.name",
                "spring.mvc.problemdetails.enabled", "logging.structured.format.console"}) {
            System.out.println(name + "=" + environment.getProperty(name, "<absent>"));
        }
        environment.getPropertySources().forEach(source ->
            System.out.println("source=" + source.getName()));
    }
}
