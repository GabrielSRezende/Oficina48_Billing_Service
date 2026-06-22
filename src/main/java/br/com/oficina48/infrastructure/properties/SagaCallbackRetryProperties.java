package br.com.oficina48.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.saga.callback-retry")
public record SagaCallbackRetryProperties(
        long fixedDelay,
        long initialDelay,
        int maxAttempts
) {
}
