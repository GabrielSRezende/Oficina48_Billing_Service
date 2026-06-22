package br.com.oficina48.infrastructure.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.os-service")
public record OsServiceProperties(
        String baseUrl,
        String callbackPath,
        Duration connectTimeout,
        Duration readTimeout
) {
}
