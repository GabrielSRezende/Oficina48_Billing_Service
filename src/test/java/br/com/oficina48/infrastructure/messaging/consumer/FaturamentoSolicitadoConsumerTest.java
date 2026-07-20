package br.com.oficina48.infrastructure.messaging.consumer;

import br.com.oficina48.application.usecase.SolicitarFaturamentoUseCase;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoSolicitadoEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaturamentoSolicitadoConsumerTest {

    @Mock
    private SolicitarFaturamentoUseCase solicitarFaturamentoUseCase;

    private FaturamentoSolicitadoConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new FaturamentoSolicitadoConsumer(solicitarFaturamentoUseCase);
    }

    @Test
    @DisplayName("receber - Deve delegar execução para SolicitarFaturamentoUseCase com sucesso")
    void deveDelegarExecucaoComSucesso() {
        FaturamentoSolicitadoEvent event = new FaturamentoSolicitadoEvent(
                1L, "saga-1", BigDecimal.TEN, "email", "nome", "cpf", "desc"
        );

        consumer.receber(event);

        verify(solicitarFaturamentoUseCase).executar(event);
    }

    @Test
    @DisplayName("receber - Se UseCase falhar, deve propagar a exceção")
    void devePropagarExcecaoSeUseCaseFalhar() {
        FaturamentoSolicitadoEvent event = new FaturamentoSolicitadoEvent(
                1L, "saga-1", BigDecimal.TEN, "email", "nome", "cpf", "desc"
        );

        doThrow(new RuntimeException("Database error")).when(solicitarFaturamentoUseCase).executar(event);

        assertThrows(RuntimeException.class, () -> consumer.receber(event));
        verify(solicitarFaturamentoUseCase).executar(event);
    }
}
