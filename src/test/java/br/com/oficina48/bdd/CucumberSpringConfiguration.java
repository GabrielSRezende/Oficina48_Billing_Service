package br.com.oficina48.bdd;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {

    @TestConfiguration
    static class BddTestConfig {

        @Bean
        @Primary
        public SqsAsyncClient sqsAsyncClient() {
            SqsAsyncClient client = mock(SqsAsyncClient.class);
            GetQueueUrlResponse mockResponse = GetQueueUrlResponse.builder()
                    .queueUrl("http://localhost:4566/000000000000/mock-queue")
                    .build();

            when(client.getQueueUrl(any(GetQueueUrlRequest.class)))
                    .thenReturn(CompletableFuture.completedFuture(mockResponse));

            return client;
        }

        @Bean
        @Primary
        public SqsTemplate sqsTemplate() {
            return mock(SqsTemplate.class);
        }
    }
}
