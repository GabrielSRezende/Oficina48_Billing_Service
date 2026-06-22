package br.com.oficina48;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@SpringBootTest
@Import(BillingServiceApplicationTests.SqsTestConfig.class)
class BillingServiceApplicationTests {

    @TestConfiguration
    static class SqsTestConfig {
        @Bean
        public SqsAsyncClient sqsAsyncClient() {
            SqsAsyncClient client = Mockito.mock(SqsAsyncClient.class);

            Mockito.when(client.getQueueUrl(Mockito.any(GetQueueUrlRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(
                    GetQueueUrlResponse.builder()
                        .queueUrl("http://localhost:4566/000000000000/mock-queue")
                        .build()
                ));

            Mockito.when(client.getQueueAttributes(Mockito.any(GetQueueAttributesRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(
                    GetQueueAttributesResponse.builder()
                        .attributes(Map.of())
                        .build()
                ));

            Mockito.when(client.receiveMessage(Mockito.any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(
                    ReceiveMessageResponse.builder()
                        .messages(Collections.emptyList())
                        .build()
                ));

            Mockito.when(client.sendMessage(Mockito.any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(
                    SendMessageResponse.builder()
                        .messageId("mock-msg-id")
                        .build()
                ));

            return client;
        }
    }

    @Test
    void contextLoads() {
    }
}
