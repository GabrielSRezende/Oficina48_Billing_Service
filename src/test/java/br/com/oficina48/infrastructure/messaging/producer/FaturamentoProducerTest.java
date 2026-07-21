package br.com.oficina48.infrastructure.messaging.producer;

import br.com.oficina48.infrastructure.messaging.EventMapper;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoConcluidoEvent;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoFalhouEvent;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoPendenteEvent;
import br.com.oficina48.infrastructure.properties.SqsProperties;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaturamentoProducerTest {

    @Mock
    private SqsTemplate sqsTemplate;

    @Mock
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
        producer = new FaturamentoProducer(sqsTemplate, sqsProperties, eventMapper);
    }

    @Test
    @DisplayName("enviarFaturamentoPendente - Deve serializar e enviar evento com sucesso")
    void deveEnviarFaturamentoPendenteComSucesso() {
        FaturamentoPendenteEvent event = new FaturamentoPendenteEvent(
                1L, "saga-1", "mp-1", "http://link", BigDecimal.TEN
        );

        when(eventMapper.toJson(event)).thenReturn("{\"sagaId\":\"saga-1\"}");

        producer.enviarFaturamentoPendente(event);

        verify(sqsTemplate).send(any(Consumer.class));
    }

    @Test
    @DisplayName("enviarFaturamentoPendente - Se falhar na serialização, deve lançar RuntimeException")
    void deveLancarErroAoFalharSerializacaoPendente() {
        FaturamentoPendenteEvent event = new FaturamentoPendenteEvent(
                1L, "saga-1", "mp-1", "http://link", BigDecimal.TEN
        );

        when(eventMapper.toJson(event)).thenThrow(new RuntimeException("Erro ao serializar"));

        assertThrows(RuntimeException.class, () -> producer.enviarFaturamentoPendente(event));
    }

    @Test
    @DisplayName("enviarFaturamentoConcluido - Deve serializar e enviar evento com sucesso")
    void deveEnviarFaturamentoConcluidoComSucesso() {
        FaturamentoConcluidoEvent event = new FaturamentoConcluidoEvent(
                1L, "saga-1", "mp-1", BigDecimal.TEN
        );

        when(eventMapper.toJson(event)).thenReturn("{\"sagaId\":\"saga-1\"}");

        producer.enviarFaturamentoConcluido(event);

        verify(sqsTemplate).send(any(Consumer.class));
    }

    @Test
    @DisplayName("enviarFaturamentoConcluido - Se falhar na serialização, deve lançar RuntimeException")
    void deveLancarErroAoFalharSerializacaoConcluido() {
        FaturamentoConcluidoEvent event = new FaturamentoConcluidoEvent(
                1L, "saga-1", "mp-1", BigDecimal.TEN
        );

        when(eventMapper.toJson(event)).thenThrow(new RuntimeException("Erro ao serializar"));

        assertThrows(RuntimeException.class, () -> producer.enviarFaturamentoConcluido(event));
    }

    @Test
    @DisplayName("enviarFaturamentoFalhou - Deve serializar e enviar evento com sucesso")
    void deveEnviarFaturamentoFalhouComSucesso() {
        FaturamentoFalhouEvent event = new FaturamentoFalhouEvent(
                1L, "saga-1", "Erro", BigDecimal.TEN
        );

        when(eventMapper.toJson(event)).thenReturn("{\"sagaId\":\"saga-1\"}");

        producer.enviarFaturamentoFalhou(event);

        verify(sqsTemplate).send(any(Consumer.class));
    }

    @Test
    @DisplayName("enviarFaturamentoFalhou - Se falhar na serialização, deve lançar RuntimeException")
    void deveLancarErroAoFalharSerializacaoFalhou() {
        FaturamentoFalhouEvent event = new FaturamentoFalhouEvent(
                1L, "saga-1", "Erro", BigDecimal.TEN
        );

        when(eventMapper.toJson(event)).thenThrow(new RuntimeException("Erro ao serializar"));

        assertThrows(RuntimeException.class, () -> producer.enviarFaturamentoFalhou(event));
    }
}
