package br.com.oficina48.infrastructure.messaging.producer;

import br.com.oficina48.infrastructure.messaging.EventMapper;
import br.com.oficina48.infrastructure.messaging.event.OrcamentoAprovadoEvent;
import br.com.oficina48.infrastructure.messaging.event.OrcamentoReprovadoEvent;
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

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrcamentoProducerTest {

    @Mock
    private SqsTemplate sqsTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private EventMapper eventMapper;

    private SqsProperties sqsProperties;
    private OrcamentoProducer producer;

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
        producer = new OrcamentoProducer(sqsTemplate, sqsProperties, eventMapper);
    }

    @Test
    @DisplayName("enviarOrcamentoAprovado - Deve serializar e enviar evento com sucesso")
    void deveEnviarOrcamentoAprovadoComSucesso() throws JsonProcessingException {
        OrcamentoAprovadoEvent event = new OrcamentoAprovadoEvent(1L, "saga-1");

        when(objectMapper.writeValueAsString(event)).thenReturn("{\"sagaId\":\"saga-1\"}");

        producer.enviarOrcamentoAprovado(event);

        verify(sqsTemplate).send(any(Consumer.class));
    }

    @Test
    @DisplayName("enviarOrcamentoAprovado - Se falhar na serialização, deve lançar RuntimeException")
    void deveLancarErroAoFalharSerializacaoAprovado() throws JsonProcessingException {
        OrcamentoAprovadoEvent event = new OrcamentoAprovadoEvent(1L, "saga-1");

        JsonProcessingException mockException = mock(JsonProcessingException.class);
        when(objectMapper.writeValueAsString(event)).thenThrow(mockException);

        assertThrows(RuntimeException.class, () -> producer.enviarOrcamentoAprovado(event));
        verifyNoInteractions(sqsTemplate);
    }

    @Test
    @DisplayName("enviarOrcamentoReprovado - Deve serializar e enviar evento com sucesso")
    void deveEnviarOrcamentoReprovadoComSucesso() throws JsonProcessingException {
        OrcamentoReprovadoEvent event = new OrcamentoReprovadoEvent(1L, "saga-1", null);

        when(objectMapper.writeValueAsString(event)).thenReturn("{\"sagaId\":\"saga-1\"}");

        producer.enviarOrcamentoReprovado(event);

        verify(sqsTemplate).send(any(Consumer.class));
    }

    @Test
    @DisplayName("enviarOrcamentoReprovado - Se falhar na serialização, deve lançar RuntimeException")
    void deveLancarErroAoFalharSerializacaoReprovado() throws JsonProcessingException {
        OrcamentoReprovadoEvent event = new OrcamentoReprovadoEvent(1L, "saga-1", null);

        JsonProcessingException mockException = mock(JsonProcessingException.class);
        when(objectMapper.writeValueAsString(event)).thenThrow(mockException);

        assertThrows(RuntimeException.class, () -> producer.enviarOrcamentoReprovado(event));
        verifyNoInteractions(sqsTemplate);
    }
}
