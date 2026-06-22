package br.com.oficina48.infrastructure.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsSqsConfig {
    // Relying on Spring Cloud AWS starter autoconfiguration for SqsTemplate and SqsListenerContainerFactory.
}
