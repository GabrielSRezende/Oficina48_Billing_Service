package br.com.oficina48.application.usecase;

import br.com.oficina48.domain.model.Orcamento;
import br.com.oficina48.domain.model.OrcamentoStatus;
import br.com.oficina48.domain.repository.OrcamentoRepository;
import br.com.oficina48.infrastructure.messaging.event.OrcamentoAprovadoEvent;
import br.com.oficina48.infrastructure.messaging.event.OrcamentoReprovadoEvent;
import br.com.oficina48.infrastructure.messaging.event.OrcamentoSolicitadoEvent;
import br.com.oficina48.infrastructure.messaging.event.ItemOrcamentoEvent;
import br.com.oficina48.infrastructure.messaging.producer.OrcamentoProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrcamentoUseCasesTest {

    @Mock
    private OrcamentoRepository orcamentoRepository;

    @Mock
    private OrcamentoProducer orcamentoProducer;

    private SolicitarOrcamentoUseCase solicitarOrcamentoUseCase;
    private ProcessarDecisaoOrcamentoUseCase processarDecisaoOrcamentoUseCase;

    @BeforeEach
    void setUp() {
        solicitarOrcamentoUseCase = new SolicitarOrcamentoUseCase(orcamentoRepository);
        processarDecisaoOrcamentoUseCase = new ProcessarDecisaoOrcamentoUseCase(orcamentoRepository, orcamentoProducer);
    }

    @Test
    @DisplayName("Solicitar Orçamento - Deve salvar orçamento com status PENDENTE")
    void deveSolicitarOrcamentoComSucesso() {
        OrcamentoSolicitadoEvent event = new OrcamentoSolicitadoEvent(
                10L, 2L, "ABC-1234",
                Collections.singletonList(new ItemOrcamentoEvent("Serviço A", BigDecimal.ONE, BigDecimal.valueOf(100.00), BigDecimal.valueOf(100.00))),
                Collections.emptyList(),
                Collections.emptyList(),
                BigDecimal.valueOf(100.00)
        );

        when(orcamentoRepository.findFirstByOrdemServicoIdOrderByDataCriacaoDesc(10L))
                .thenReturn(Optional.empty());

        solicitarOrcamentoUseCase.executar(event);

        ArgumentCaptor<Orcamento> captor = ArgumentCaptor.forClass(Orcamento.class);
        verify(orcamentoRepository).save(captor.capture());

        Orcamento saved = captor.getValue();
        assertEquals(10L, saved.getOrdemServicoId());
        assertEquals(BigDecimal.valueOf(100.00), saved.getValorTotal());
        assertEquals(OrcamentoStatus.PENDENTE, saved.getStatus());
    }

    @Test
    @DisplayName("Processar Decisão - Deve aprovar orçamento e notificar OS Service")
    void deveAprovarOrcamentoComSucesso() {
        Orcamento orcamento = Orcamento.builder()
                .id(1L)
                .ordemServicoId(10L)
                .valorTotal(BigDecimal.valueOf(100.00))
                .status(OrcamentoStatus.PENDENTE)
                .build();

        when(orcamentoRepository.findById(1L)).thenReturn(Optional.of(orcamento));

        processarDecisaoOrcamentoUseCase.executar(1L, true);

        assertEquals(OrcamentoStatus.APROVADO, orcamento.getStatus());
        verify(orcamentoRepository).save(orcamento);
        verify(orcamentoProducer).enviarOrcamentoAprovado(new OrcamentoAprovadoEvent(10L));
        verify(orcamentoProducer, never()).enviarOrcamentoReprovado(any());
    }

    @Test
    @DisplayName("Processar Decisão - Deve reprovar orçamento e notificar OS Service")
    void deveReprovarOrcamentoComSucesso() {
        Orcamento orcamento = Orcamento.builder()
                .id(1L)
                .ordemServicoId(10L)
                .valorTotal(BigDecimal.valueOf(100.00))
                .status(OrcamentoStatus.PENDENTE)
                .build();

        when(orcamentoRepository.findById(1L)).thenReturn(Optional.of(orcamento));

        processarDecisaoOrcamentoUseCase.executar(1L, false);

        assertEquals(OrcamentoStatus.REPROVADO, orcamento.getStatus());
        verify(orcamentoRepository).save(orcamento);
        verify(orcamentoProducer).enviarOrcamentoReprovado(new OrcamentoReprovadoEvent(10L));
        verify(orcamentoProducer, never()).enviarOrcamentoAprovado(any());
    }

    @Test
    @DisplayName("Processar Decisão - Deve lançar exceção se orçamento não estiver PENDENTE")
    void deveLancarExcecaoSeStatusNaoForPendente() {
        Orcamento orcamento = Orcamento.builder()
                .id(1L)
                .ordemServicoId(10L)
                .status(OrcamentoStatus.APROVADO)
                .build();

        when(orcamentoRepository.findById(1L)).thenReturn(Optional.of(orcamento));

        assertThrows(IllegalStateException.class, () -> 
                processarDecisaoOrcamentoUseCase.executar(1L, true)
        );

        verify(orcamentoRepository, never()).save(any());
        verify(orcamentoProducer, never()).enviarOrcamentoAprovado(any());
    }
}
