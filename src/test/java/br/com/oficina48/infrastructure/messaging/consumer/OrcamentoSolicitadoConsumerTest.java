package br.com.oficina48.infrastructure.messaging.consumer;

import br.com.oficina48.application.usecase.SolicitarOrcamentoUseCase;
import br.com.oficina48.infrastructure.messaging.event.OrcamentoSolicitadoEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrcamentoSolicitadoConsumerTest {

    @Mock
    private SolicitarOrcamentoUseCase solicitarOrcamentoUseCase;

    private OrcamentoSolicitadoConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new OrcamentoSolicitadoConsumer(solicitarOrcamentoUseCase);
    }

    @Test
    @DisplayName("receber - Deve delegar execução para SolicitarOrcamentoUseCase com sucesso")
    void deveDelegarExecucaoComSucesso() {
        OrcamentoSolicitadoEvent event = new OrcamentoSolicitadoEvent(
                10L, 20L, "ABC-1234", "saga-10", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), BigDecimal.TEN
        );

        consumer.receber(event);

        verify(solicitarOrcamentoUseCase).executar(event);
    }

    @Test
    @DisplayName("receber - Se UseCase falhar, deve propagar a exceção")
    void devePropagarExcecaoSeUseCaseFalhar() {
        OrcamentoSolicitadoEvent event = new OrcamentoSolicitadoEvent(
                10L, 20L, "ABC-1234", "saga-10", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), BigDecimal.TEN
        );

        doThrow(new RuntimeException("Storage error")).when(solicitarOrcamentoUseCase).executar(event);

        assertThrows(RuntimeException.class, () -> consumer.receber(event));
        verify(solicitarOrcamentoUseCase).executar(event);
    }
}
