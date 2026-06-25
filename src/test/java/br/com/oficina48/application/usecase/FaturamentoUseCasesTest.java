package br.com.oficina48.application.usecase;

import br.com.oficina48.domain.model.Faturamento;
import br.com.oficina48.domain.model.FaturamentoStatus;
import br.com.oficina48.domain.repository.FaturamentoRepository;
import br.com.oficina48.infrastructure.integration.mercadopago.BankProvider;
import br.com.oficina48.infrastructure.integration.mercadopago.dto.ChargeRequest;
import br.com.oficina48.infrastructure.integration.mercadopago.dto.ChargeResponse;
import br.com.oficina48.infrastructure.integration.mercadopago.dto.ChargeStatus;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoConcluidoEvent;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoFalhouEvent;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoPendenteEvent;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoSolicitadoEvent;
import br.com.oficina48.infrastructure.messaging.producer.FaturamentoProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaturamentoUseCasesTest {

    @Mock
    private FaturamentoRepository faturamentoRepository;

    @Mock
    private BankProvider bankProvider;

    @Mock
    private FaturamentoProducer faturamentoProducer;

    private SolicitarFaturamentoUseCase solicitarUseCase;
    private ConfirmarPagamentoFaturamentoUseCase confirmarUseCase;

    @BeforeEach
    void setUp() {
        solicitarUseCase = new SolicitarFaturamentoUseCase(faturamentoRepository, bankProvider, faturamentoProducer);
        confirmarUseCase = new ConfirmarPagamentoFaturamentoUseCase(faturamentoRepository, bankProvider, faturamentoProducer);
    }

    @Test
    @DisplayName("Solicitar Faturamento - Deve criar cobrança pendente com sucesso")
    void deveSolicitarFaturamentoComSucesso() {
        FaturamentoSolicitadoEvent event = new FaturamentoSolicitadoEvent(
                1L, BigDecimal.valueOf(150.00), "cliente@email.com", "Cliente Teste", "12345678901", "OS 1 Description"
        );

        when(faturamentoRepository.findFirstByOrdemServicoIdOrderByDataCriacaoDesc(1L))
                .thenReturn(Optional.empty());

        ChargeResponse chargeResponse = ChargeResponse.builder()
                .transactionId("mp-12345")
                .paymentLink("http://pagamento.link")
                .externalReference("1")
                .status("pending")
                .build();

        when(bankProvider.createPixCharge(any(ChargeRequest.class))).thenReturn(chargeResponse);

        solicitarUseCase.executar(event);

        ArgumentCaptor<Faturamento> faturamentoCaptor = ArgumentCaptor.forClass(Faturamento.class);
        verify(faturamentoRepository).save(faturamentoCaptor.capture());

        Faturamento saved = faturamentoCaptor.getValue();
        assertEquals(1L, saved.getOrdemServicoId());
        assertEquals(BigDecimal.valueOf(150.00), saved.getValor());
        assertEquals(FaturamentoStatus.PENDENTE, saved.getStatus());
        assertEquals("mp-12345", saved.getPagamentoId());
        assertEquals("http://pagamento.link", saved.getPagamentoLink());

        verify(faturamentoProducer).enviarFaturamentoPendente(new FaturamentoPendenteEvent(
                1L, "mp-12345", "http://pagamento.link", BigDecimal.valueOf(150.00)
        ));
        verify(faturamentoProducer, never()).enviarFaturamentoConcluido(any());
        verify(faturamentoProducer, never()).enviarFaturamentoFalhou(any());
    }

    @Test
    @DisplayName("Solicitar Faturamento - Se já concluído, deve apenas reenviar evento")
    void deveReenviarEventoSeJaConcluido() {
        FaturamentoSolicitadoEvent event = new FaturamentoSolicitadoEvent(
                1L, BigDecimal.valueOf(150.00), "cliente@email.com", "Cliente Teste", "12345678901", "OS 1 Description"
        );

        Faturamento faturamentoExistente = Faturamento.builder()
                .ordemServicoId(1L)
                .valor(BigDecimal.valueOf(150.00))
                .status(FaturamentoStatus.CONCLUIDO)
                .pagamentoId("mp-already-paid")
                .build();

        when(faturamentoRepository.findFirstByOrdemServicoIdOrderByDataCriacaoDesc(1L))
                .thenReturn(Optional.of(faturamentoExistente));

        solicitarUseCase.executar(event);

        verify(bankProvider, never()).createPixCharge(any());
        verify(faturamentoRepository, never()).save(any());

        verify(faturamentoProducer).enviarFaturamentoConcluido(new FaturamentoConcluidoEvent(
                1L, "mp-already-paid", BigDecimal.valueOf(150.00)
        ));
    }

    @Test
    @DisplayName("Solicitar Faturamento - Se falhar no gateway, deve salvar como falhado e enviar evento")
    void deveSalvarComoFalhadoSeGatewayFalhar() {
        FaturamentoSolicitadoEvent event = new FaturamentoSolicitadoEvent(
                1L, BigDecimal.valueOf(150.00), "cliente@email.com", "Cliente Teste", "12345678901", "OS 1 Description"
        );

        when(faturamentoRepository.findFirstByOrdemServicoIdOrderByDataCriacaoDesc(1L))
                .thenReturn(Optional.empty());

        when(bankProvider.createPixCharge(any(ChargeRequest.class)))
                .thenThrow(new RuntimeException("MP API offline"));

        solicitarUseCase.executar(event);

        ArgumentCaptor<Faturamento> faturamentoCaptor = ArgumentCaptor.forClass(Faturamento.class);
        verify(faturamentoRepository).save(faturamentoCaptor.capture());

        Faturamento saved = faturamentoCaptor.getValue();
        assertEquals(1L, saved.getOrdemServicoId());
        assertEquals(FaturamentoStatus.FALHOU, saved.getStatus());

        verify(faturamentoProducer).enviarFaturamentoFalhou(any(FaturamentoFalhouEvent.class));
    }

    @Test
    @DisplayName("Confirmar Pagamento - Deve marcar como concluído quando aprovado")
    void deveConfirmarPagamentoComSucesso() {
        ChargeStatus status = ChargeStatus.builder()
                .status("approved")
                .paid(true)
                .externalReference("1")
                .transactionId("mp-12345")
                .build();

        Faturamento faturamento = Faturamento.builder()
                .ordemServicoId(1L)
                .valor(BigDecimal.valueOf(150.00))
                .status(FaturamentoStatus.PENDENTE)
                .pagamentoId("mp-12345")
                .build();

        when(bankProvider.checkStatus("mp-12345", null)).thenReturn(status);
        when(faturamentoRepository.findByPagamentoId("mp-12345")).thenReturn(Optional.of(faturamento));

        confirmarUseCase.confirmarPorMercadoPagoId("mp-12345");

        assertEquals(FaturamentoStatus.CONCLUIDO, faturamento.getStatus());
        verify(faturamentoRepository).save(faturamento);
        verify(faturamentoProducer).enviarFaturamentoConcluido(new FaturamentoConcluidoEvent(
                1L, "mp-12345", BigDecimal.valueOf(150.00)
        ));
    }

    @Test
    @DisplayName("Confirmar Pagamento - Deve marcar como falhado quando rejeitado")
    void deveMarcarComoFalhadoQuandoRejeitado() {
        ChargeStatus status = ChargeStatus.builder()
                .status("rejected")
                .paid(false)
                .details("cc_rejected_insufficient_amount")
                .externalReference("1")
                .transactionId("mp-12345")
                .build();

        Faturamento faturamento = Faturamento.builder()
                .ordemServicoId(1L)
                .valor(BigDecimal.valueOf(150.00))
                .status(FaturamentoStatus.PENDENTE)
                .pagamentoId("mp-12345")
                .build();

        when(bankProvider.checkStatus("mp-12345", null)).thenReturn(status);
        when(faturamentoRepository.findByPagamentoId("mp-12345")).thenReturn(Optional.of(faturamento));

        confirmarUseCase.confirmarPorMercadoPagoId("mp-12345");

        assertEquals(FaturamentoStatus.FALHOU, faturamento.getStatus());
        verify(faturamentoRepository).save(faturamento);
        verify(faturamentoProducer).enviarFaturamentoFalhou(any(FaturamentoFalhouEvent.class));
    }
}
