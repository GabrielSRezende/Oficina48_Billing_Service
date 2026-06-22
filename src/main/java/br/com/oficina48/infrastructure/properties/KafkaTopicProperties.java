package br.com.oficina48.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public record KafkaTopicProperties(
        String faturamentoSolicitado,
        String faturamentoConcluido,
        String faturamentoFalhou
) {
}
