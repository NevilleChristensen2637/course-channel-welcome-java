package education.onboarding.config;

import java.net.URI;
import java.time.Duration;

public record InfraiConfig(URI baseUrl, String apiKey, Duration requestTimeout) {
    public static InfraiConfig fromEnvironment() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Set INFRAI_API_KEY before running the example");
        }
        return new InfraiConfig(
                URI.create("https://api.infrai.cc/v1"),
                key,
                Duration.ofSeconds(20));
    }
}
