package br.com.oficina48.infrastructure.messaging.producer;

import br.com.oficina48.domain.model.MotivoErro;
import br.com.oficina48.infrastructure.messaging.EventMapper;
import br.com.oficina48.infrastructure.messaging.event.OrcamentoAprovadoEvent;
import br.com.oficina48.infrastructure.messaging.event.OrcamentoReprovadoEvent;
import br.com.oficina48.infrastructure.properties.SqsProperties;
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
        producer = new OrcamentoProducer(sqsTemplate, sqsProperties, eventMapper);
    }

    @Test
    @DisplayName("enviarOrcamentoAprovado - Deve serializar e enviar evento com sucesso")
    void deveEnviarOrcamentoAprovadoComSucesso() {
        OrcamentoAprovadoEvent event = new OrcamentoAprovadoEvent(1L, "saga-1");

        when(eventMapper.toJson(event)).thenReturn("{\"sagaId\":\"saga-1\"}");

        producer.enviarOrcamentoAprovado(event);

        verify(sqsTemplate).send(any(Consumer.class));
    }

    @Test
    @DisplayName("enviarOrcamentoAprovado - Se falhar na serialização, deve lançar RuntimeException")
    void deveLancarErroAoFalharSerializacaoAprovado() {
        OrcamentoAprovadoEvent event = new OrcamentoAprovadoEvent(1L, "saga-1");

        when(eventMapper.toJson(event)).thenThrow(new RuntimeException("Erro ao serializar"));

        assertThrows(RuntimeException.class, () -> producer.enviarOrcamentoAprovado(event));
    }

    @Test
    @DisplayName("enviarOrcamentoReprovado - Deve serializar e enviar evento com sucesso")
    void deveEnviarOrcamentoReprovadoComSucesso() {
        OrcamentoReprovadoEvent event = new OrcamentoReprovadoEvent(1L, "saga-1", MotivoErro.ORCAMENTO_REJEITADO_PELO_CLIENTE);

        when(eventMapper.toJson(event)).thenReturn("{\"sagaId\":\"saga-1\"}");

        producer.enviarOrcamentoReprovado(event);

        verify(sqsTemplate).send(any(Consumer.class));
    }

    @Test
    @DisplayName("enviarOrcamentoReprovado - Se falhar na serialização, deve lançar RuntimeException")
    void deveLancarErroAoFalharSerializacaoReprovado() {
        OrcamentoReprovadoEvent event = new OrcamentoReprovadoEvent(1L, "saga-1", MotivoErro.ORCAMENTO_REJEITADO_PELO_CLIENTE);

        when(eventMapper.toJson(event)).thenThrow(new RuntimeException("Erro ao serializar"));

        assertThrows(RuntimeException.class, () -> producer.enviarOrcamentoReprovado(event));
    }
}
