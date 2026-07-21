package br.com.oficina48.infrastructure.messaging.producer;

import br.com.oficina48.infrastructure.messaging.EventMapper;
import br.com.oficina48.infrastructure.messaging.event.FalhaPagamentoEvent;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoConcluidoEvent;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoFalhouEvent;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoPendenteEvent;
import br.com.oficina48.infrastructure.properties.SqsProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaturamentoProducerTest {

    @Mock
    private SqsTemplate sqsTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private EventMapper eventMapper;

    private SqsProperties sqsProperties;
    private FaturamentoProducer producer;

    @BeforeEach
    void setUp() {
        SqsProperties.Queues queues = new SqsProperties.Queues(
                "faturamento-solicitado-queue",
                "faturamento-pendente-queue",
                "faturamento-concluido-queue",
                "faturamento-falhou-queue",
                "falha-pagamento-queue",
                "orcamento-solicitado-queue",
                "orcamento-aprovado-queue",
                "orcamento-reprovado-queue"
        );
        sqsProperties = new SqsProperties(queues);
        eventMapper = new EventMapper(objectMapper);
        producer = new FaturamentoProducer(sqsTemplate, sqsProperties, eventMapper);
    }

    @Test
    @DisplayName("enviarFaturamentoPendente - Deve serializar e enviar evento com sucesso")
    void deveEnviarFaturamentoPendenteComSucesso() throws JsonProcessingException {
        FaturamentoPendenteEvent event = new FaturamentoPendenteEvent(
                1L, "saga-1", "mp-1", "http://link", BigDecimal.TEN
        );

        when(objectMapper.writeValueAsString(event)).thenReturn("{\"sagaId\":\"saga-1\"}");

        producer.enviarFaturamentoPendente(event);

        verify(sqsTemplate).send(any(Consumer.class));
    }

    @Test
    @DisplayName("enviarFaturamentoPendente - Se falhar na serialização, deve lançar RuntimeException")
    void deveLancarErroAoFalharSerializacaoPendente() throws JsonProcessingException {
        FaturamentoPendenteEvent event = new FaturamentoPendenteEvent(
                1L, "saga-1", "mp-1", "http://link", BigDecimal.TEN
        );

        JsonProcessingException mockException = mock(JsonProcessingException.class);
        when(objectMapper.writeValueAsString(event)).thenThrow(mockException);

        assertThrows(RuntimeException.class, () -> producer.enviarFaturamentoPendente(event));
        verifyNoInteractions(sqsTemplate);
    }

    @Test
    @DisplayName("enviarFaturamentoConcluido - Deve serializar e enviar evento com sucesso")
    void deveEnviarFaturamentoConcluidoComSucesso() throws JsonProcessingException {
        FaturamentoConcluidoEvent event = new FaturamentoConcluidoEvent(
                1L, "saga-1", "mp-1", BigDecimal.TEN
        );

        when(objectMapper.writeValueAsString(event)).thenReturn("{\"sagaId\":\"saga-1\"}");

        producer.enviarFaturamentoConcluido(event);

        verify(sqsTemplate).send(any(Consumer.class));
    }

    @Test
    @DisplayName("enviarFaturamentoConcluido - Se falhar na serialização, deve lançar RuntimeException")
    void deveLancarErroAoFalharSerializacaoConcluido() throws JsonProcessingException {
        FaturamentoConcluidoEvent event = new FaturamentoConcluidoEvent(
                1L, "saga-1", "mp-1", BigDecimal.TEN
        );

        JsonProcessingException mockException = mock(JsonProcessingException.class);
        when(objectMapper.writeValueAsString(event)).thenThrow(mockException);

        assertThrows(RuntimeException.class, () -> producer.enviarFaturamentoConcluido(event));
        verifyNoInteractions(sqsTemplate);
    }

    @Test
    @DisplayName("enviarFaturamentoFalhou - Deve serializar e enviar evento com sucesso")
    void deveEnviarFaturamentoFalhouComSucesso() throws JsonProcessingException {
        FaturamentoFalhouEvent event = new FaturamentoFalhouEvent(
                1L, "saga-1", "Erro", BigDecimal.TEN
        );

        when(objectMapper.writeValueAsString(event)).thenReturn("{\"sagaId\":\"saga-1\"}");

        producer.enviarFaturamentoFalhou(event);

        verify(sqsTemplate).send(any(Consumer.class));
    }

    @Test
    @DisplayName("enviarFaturamentoFalhou - Se falhar na serialização, deve lançar RuntimeException")
    void deveLancarErroAoFalharSerializacaoFalhou() throws JsonProcessingException {
        FaturamentoFalhouEvent event = new FaturamentoFalhouEvent(
                1L, "saga-1", "Erro", BigDecimal.TEN
        );

        JsonProcessingException mockException = mock(JsonProcessingException.class);
        when(objectMapper.writeValueAsString(event)).thenThrow(mockException);

        assertThrows(RuntimeException.class, () -> producer.enviarFaturamentoFalhou(event));
        verifyNoInteractions(sqsTemplate);
    }

    @Test
    @DisplayName("enviarFalhaPagamento - Deve serializar e enviar evento com sucesso")
    void deveEnviarFalhaPagamentoComSucesso() throws JsonProcessingException {
        FalhaPagamentoEvent event = new FalhaPagamentoEvent(
                1L, "saga-1", BigDecimal.TEN, "MRCPAGO", "MP_API_EXCEPTION", "Falha no gateway"
        );

        when(objectMapper.writeValueAsString(event)).thenReturn("{\"sagaId\":\"saga-1\"}");

        producer.enviarFalhaPagamento(event);

        verify(sqsTemplate).send(any(Consumer.class));
    }

    @Test
    @DisplayName("enviarFalhaPagamento - Se falhar na serialização, deve lançar RuntimeException")
    void deveLancarErroAoFalharSerializacaoFalhaPagamento() throws JsonProcessingException {
        FalhaPagamentoEvent event = new FalhaPagamentoEvent(
                1L, "saga-1", BigDecimal.TEN, "MRCPAGO", "MP_API_EXCEPTION", "Falha no gateway"
        );

        JsonProcessingException mockException = mock(JsonProcessingException.class);
        when(objectMapper.writeValueAsString(event)).thenThrow(mockException);

        assertThrows(RuntimeException.class, () -> producer.enviarFalhaPagamento(event));
        verifyNoInteractions(sqsTemplate);
    }
}
