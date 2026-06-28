package br.com.oficina48.infrastructure.http.config;

import br.com.oficina48.infrastructure.properties.OsServiceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class OsServiceClientConfig {

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    RestClient osServiceRestClient(RestClient.Builder builder, OsServiceProperties osServiceProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.toIntExact(osServiceProperties.connectTimeout().toMillis()));
        requestFactory.setReadTimeout(Math.toIntExact(osServiceProperties.readTimeout().toMillis()));

        return builder
                .baseUrl(osServiceProperties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
