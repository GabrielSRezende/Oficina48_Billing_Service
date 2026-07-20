package br.com.oficina48.application.usecase;

import br.com.oficina48.domain.model.Faturamento;
import br.com.oficina48.domain.model.FaturamentoStatus;
import br.com.oficina48.domain.repository.FaturamentoRepository;
import br.com.oficina48.infrastructure.integration.mercadopago.BankProvider;
import br.com.oficina48.infrastructure.integration.mercadopago.FalhaPagamentoException;
import br.com.oficina48.infrastructure.integration.mercadopago.dto.ChargeRequest;
import br.com.oficina48.infrastructure.integration.mercadopago.dto.ChargeResponse;
import br.com.oficina48.infrastructure.integration.mercadopago.dto.ChargeStatus;
import br.com.oficina48.infrastructure.messaging.event.FalhaPagamentoEvent;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoConcluidoEvent;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoFalhouEvent;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoPendenteEvent;
import br.com.oficina48.application.service.DocumentoStorage;
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

    @Mock
    private DocumentoStorage documentoStorage;

    private SolicitarFaturamentoUseCase solicitarUseCase;
    private ConfirmarPagamentoFaturamentoUseCase confirmarUseCase;

    @BeforeEach
    void setUp() {
        solicitarUseCase = new SolicitarFaturamentoUseCase(faturamentoRepository, bankProvider, faturamentoProducer, documentoStorage);
        confirmarUseCase = new ConfirmarPagamentoFaturamentoUseCase(faturamentoRepository, bankProvider, faturamentoProducer, documentoStorage);
    }

    @Test
    @DisplayName("Solicitar Faturamento - Deve criar cobrança pendente com sucesso")
    void deveSolicitarFaturamentoComSucesso() {
        FaturamentoSolicitadoEvent event = new FaturamentoSolicitadoEvent(
                1L, "saga-1", BigDecimal.valueOf(150.00), "cliente@email.com", "Cliente Teste", "12345678901", "OS 1 Description"
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
        verify(documentoStorage).salvar(eq("links_pagamento"), eq("faturamento_OS_1.txt"), anyString());

        Faturamento saved = faturamentoCaptor.getValue();
        assertEquals(1L, saved.getOrdemServicoId());
        assertEquals("saga-1", saved.getSagaId());
        assertEquals(BigDecimal.valueOf(150.00), saved.getValor());
        assertEquals(FaturamentoStatus.PENDENTE, saved.getStatus());
        assertEquals("mp-12345", saved.getPagamentoId());
        assertEquals("http://pagamento.link", saved.getPagamentoLink());

        verify(faturamentoProducer).enviarFaturamentoPendente(new FaturamentoPendenteEvent(
                1L, "saga-1", "mp-12345", "http://pagamento.link", BigDecimal.valueOf(150.00)
        ));
        verify(faturamentoProducer, never()).enviarFaturamentoConcluido(any());
        verify(faturamentoProducer, never()).enviarFaturamentoFalhou(any());
        verify(faturamentoProducer, never()).enviarFalhaPagamento(any());
    }

    @Test
    @DisplayName("Solicitar Faturamento - Se já concluído, deve apenas reenviar evento")
    void deveReenviarEventoSeJaConcluido() {
        FaturamentoSolicitadoEvent event = new FaturamentoSolicitadoEvent(
                1L, "saga-1", BigDecimal.valueOf(150.00), "cliente@email.com", "Cliente Teste", "12345678901", "OS 1 Description"
        );

        Faturamento faturamentoExistente = Faturamento.builder()
                .ordemServicoId(1L)
                .sagaId("saga-1")
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
                1L, "saga-1", "mp-already-paid", BigDecimal.valueOf(150.00)
        ));
    }

    @Test
    @DisplayName("Solicitar Faturamento - Se falhar tecnicamente no Mercado Pago, deve salvar como falhado e enviar dois eventos")
    void deveSalvarComoFalhadoSeMercadoPagoFalharTecnicamente() {
        FaturamentoSolicitadoEvent event = new FaturamentoSolicitadoEvent(
                1L, "saga-1", BigDecimal.valueOf(150.00), "cliente@email.com", "Cliente Teste", "12345678901", "OS 1 Description"
        );

        when(faturamentoRepository.findFirstByOrdemServicoIdOrderByDataCriacaoDesc(1L))
                .thenReturn(Optional.empty());

        when(bankProvider.createPixCharge(any(ChargeRequest.class)))
                .thenThrow(new FalhaPagamentoException("MRCPAGO", "MP_API_EXCEPTION", "MP API offline", null));

        solicitarUseCase.executar(event);

        ArgumentCaptor<Faturamento> faturamentoCaptor = ArgumentCaptor.forClass(Faturamento.class);
        verify(faturamentoRepository).save(faturamentoCaptor.capture());

        Faturamento saved = faturamentoCaptor.getValue();
        assertEquals(1L, saved.getOrdemServicoId());
        assertEquals("saga-1", saved.getSagaId());
        assertEquals(FaturamentoStatus.FALHOU, saved.getStatus());

        verify(faturamentoProducer).enviarFaturamentoFalhou(any(FaturamentoFalhouEvent.class));
        verify(faturamentoProducer).enviarFalhaPagamento(new FalhaPagamentoEvent(
                1L,
                "saga-1",
                BigDecimal.valueOf(150.00),
                "MRCPAGO",
                "MP_API_EXCEPTION",
                "Falha ao gerar cobrança no gateway de pagamento: MP API offline"
        ));
    }

    @Test
    @DisplayName("Solicitar Faturamento - Se falhar genericamente, deve salvar como falhado e nao enviar falha-pagamento")
    void deveSalvarComoFalhadoSemEnviarFalhaPagamentoSeErroForGenerico() {
        FaturamentoSolicitadoEvent event = new FaturamentoSolicitadoEvent(
                1L, "saga-1", BigDecimal.valueOf(150.00), "cliente@email.com", "Cliente Teste", "12345678901", "OS 1 Description"
        );

        when(faturamentoRepository.findFirstByOrdemServicoIdOrderByDataCriacaoDesc(1L))
                .thenReturn(Optional.empty());

        when(bankProvider.createPixCharge(any(ChargeRequest.class)))
                .thenThrow(new RuntimeException("erro generico"));

        solicitarUseCase.executar(event);

        verify(faturamentoRepository).save(any(Faturamento.class));
        verify(faturamentoProducer).enviarFaturamentoFalhou(any(FaturamentoFalhouEvent.class));
        verify(faturamentoProducer, never()).enviarFalhaPagamento(any());
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
                .sagaId("saga-1")
                .valor(BigDecimal.valueOf(150.00))
                .status(FaturamentoStatus.PENDENTE)
                .pagamentoId("mp-12345")
                .build();

        when(bankProvider.checkStatus("mp-12345", null)).thenReturn(status);
        when(faturamentoRepository.findByPagamentoId("mp-12345")).thenReturn(Optional.of(faturamento));

        confirmarUseCase.confirmarPorMercadoPagoId("mp-12345");

        assertEquals(FaturamentoStatus.CONCLUIDO, faturamento.getStatus());
        verify(faturamentoRepository).save(faturamento);
        verify(documentoStorage).salvar(eq("links_pagamento"), eq("recibo_OS_1.txt"), anyString());
        verify(faturamentoProducer).enviarFaturamentoConcluido(new FaturamentoConcluidoEvent(
                1L, "saga-1", "mp-12345", BigDecimal.valueOf(150.00)
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
                .sagaId("saga-1")
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

    @Test
    @DisplayName("Confirmar Pagamento - Deve buscar faturamento por externalReference quando não encontrado por pagamentoId")
    void deveBuscarFaturamentoPorExternalReferenceQuandoNaoEncontradoPorPagamentoId() {
        ChargeStatus status = ChargeStatus.builder()
                .status("approved")
                .paid(true)
                .externalReference("12")
                .transactionId("mp-999")
                .build();

        Faturamento faturamento = Faturamento.builder()
                .ordemServicoId(12L)
                .sagaId("saga-12")
                .valor(BigDecimal.valueOf(100.00))
                .status(FaturamentoStatus.PENDENTE)
                .build();

        when(bankProvider.checkStatus("mp-999", null)).thenReturn(status);
        when(faturamentoRepository.findByPagamentoId("mp-999")).thenReturn(Optional.empty());
        when(faturamentoRepository.findFirstByOrdemServicoIdOrderByDataCriacaoDesc(12L))
                .thenReturn(Optional.of(faturamento));

        confirmarUseCase.confirmarPorMercadoPagoId("mp-999");

        assertEquals(FaturamentoStatus.CONCLUIDO, faturamento.getStatus());
        assertEquals("mp-999", faturamento.getPagamentoId());
        verify(faturamentoRepository).save(faturamento);
        verify(faturamentoProducer).enviarFaturamentoConcluido(any());
    }

    @Test
    @DisplayName("Confirmar Pagamento - Deve ignorar externalReference inválido (não numérico)")
    void deveIgnorarExternalReferenceInvalido() {
        ChargeStatus status = ChargeStatus.builder()
                .status("approved")
                .paid(true)
                .externalReference("not-a-number")
                .transactionId("mp-999")
                .build();

        when(bankProvider.checkStatus("mp-999", null)).thenReturn(status);
        when(faturamentoRepository.findByPagamentoId("mp-999")).thenReturn(Optional.empty());

        confirmarUseCase.confirmarPorMercadoPagoId("mp-999");

        verify(faturamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Confirmar Pagamento - Deve retornar imediatamente se o faturamento não for localizado")
    void deveRetornarSeFaturamentoNaoEncontrado() {
        ChargeStatus status = ChargeStatus.builder()
                .status("approved")
                .paid(true)
                .externalReference("12")
                .transactionId("mp-999")
                .build();

        when(bankProvider.checkStatus("mp-999", null)).thenReturn(status);
        when(faturamentoRepository.findByPagamentoId("mp-999")).thenReturn(Optional.empty());
        when(faturamentoRepository.findFirstByOrdemServicoIdOrderByDataCriacaoDesc(12L))
                .thenReturn(Optional.empty());

        confirmarUseCase.confirmarPorMercadoPagoId("mp-999");

        verify(faturamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Confirmar Pagamento - Deve retornar se o faturamento localizado já estiver CONCLUIDO")
    void deveRetornarSeFaturamentoJaConcluido() {
        ChargeStatus status = ChargeStatus.builder()
                .status("approved")
                .paid(true)
                .externalReference("12")
                .transactionId("mp-999")
                .build();

        Faturamento faturamento = Faturamento.builder()
                .ordemServicoId(12L)
                .status(FaturamentoStatus.CONCLUIDO)
                .build();

        when(bankProvider.checkStatus("mp-999", null)).thenReturn(status);
        when(faturamentoRepository.findByPagamentoId("mp-999")).thenReturn(Optional.of(faturamento));

        confirmarUseCase.confirmarPorMercadoPagoId("mp-999");

        verify(faturamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Confirmar Pagamento - Deve apenas logar e não fazer nada se o status for pendente")
    void deveIgnorarSeStatusForPendente() {
        ChargeStatus status = ChargeStatus.builder()
                .status("pending")
                .paid(false)
                .externalReference("12")
                .transactionId("mp-999")
                .build();

        Faturamento faturamento = Faturamento.builder()
                .ordemServicoId(12L)
                .status(FaturamentoStatus.PENDENTE)
                .build();

        when(bankProvider.checkStatus("mp-999", null)).thenReturn(status);
        when(faturamentoRepository.findByPagamentoId("mp-999")).thenReturn(Optional.of(faturamento));

        confirmarUseCase.confirmarPorMercadoPagoId("mp-999");

        verify(faturamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Solicitar Faturamento - Deve atualizar faturamento pendente existente em vez de criar um novo")
    void deveAtualizarFaturamentoPendenteExistente() {
        FaturamentoSolicitadoEvent event = new FaturamentoSolicitadoEvent(
                1L, "saga-1", BigDecimal.valueOf(150.00), "cliente@email.com", "Cliente Teste", "12345678901", null
        );

        Faturamento faturamentoExistente = Faturamento.builder()
                .ordemServicoId(1L)
                .sagaId("saga-old")
                .valor(BigDecimal.valueOf(100.00))
                .status(FaturamentoStatus.PENDENTE)
                .build();

        when(faturamentoRepository.findFirstByOrdemServicoIdOrderByDataCriacaoDesc(1L))
                .thenReturn(Optional.of(faturamentoExistente));

        ChargeResponse chargeResponse = ChargeResponse.builder()
                .transactionId("mp-12345")
                .paymentLink("http://pagamento.link")
                .externalReference("1")
                .status("pending")
                .build();

        when(bankProvider.createPixCharge(any(ChargeRequest.class))).thenReturn(chargeResponse);

        solicitarUseCase.executar(event);

        verify(faturamentoRepository).save(faturamentoExistente);
        assertEquals("saga-1", faturamentoExistente.getSagaId());
        assertEquals("mp-12345", faturamentoExistente.getPagamentoId());
        assertEquals(BigDecimal.valueOf(100.00), faturamentoExistente.getValor());
    }
}
