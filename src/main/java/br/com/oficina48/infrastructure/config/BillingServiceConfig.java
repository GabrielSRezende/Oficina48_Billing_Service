package br.com.oficina48.infrastructure.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BillingServiceConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
