package br.com.oficina48.infrastructure.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sqs")
public record SqsProperties(
        Queues queues
) {
    public record Queues(
            String faturamentoSolicitado,
            String faturamentoPendente,
            String faturamentoConcluido,
            String faturamentoFalhou,
            String orcamentoSolicitado,
            String orcamentoAprovado,
            String orcamentoReprovado
    ) {}
}
