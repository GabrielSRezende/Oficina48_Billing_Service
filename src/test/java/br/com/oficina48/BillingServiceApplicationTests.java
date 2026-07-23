package br.com.oficina48;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

@SpringBootTest
@Import(BillingServiceApplicationTests.SqsTestConfig.class)
class BillingServiceApplicationTests {

    @TestConfiguration
    static class SqsTestConfig {
        @Bean
        public SqsAsyncClient sqsAsyncClient() {
            SqsAsyncClient client = mock(SqsAsyncClient.class);

            when(client.getQueueUrl(any(GetQueueUrlRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(
                    GetQueueUrlResponse.builder()
                        .queueUrl("http://localhost:4566/000000000000/mock-queue")
                        .build()
                ));

            when(client.getQueueAttributes(any(GetQueueAttributesRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(
                    GetQueueAttributesResponse.builder()
                        .attributes(Map.of())
                        .build()
                ));

            when(client.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(
                    ReceiveMessageResponse.builder()
                        .messages(Collections.emptyList())
                        .build()
                ));

            when(client.sendMessage(any(SendMessageRequest.class)))
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
