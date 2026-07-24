package br.com.oficina48.application.usecase;

import br.com.oficina48.domain.model.Faturamento;
import br.com.oficina48.domain.model.FaturamentoStatus;
import br.com.oficina48.domain.repository.FaturamentoRepository;
import br.com.oficina48.infrastructure.integration.mercadopago.BankProvider;
import br.com.oficina48.infrastructure.integration.mercadopago.FalhaPagamentoException;
import br.com.oficina48.infrastructure.integration.mercadopago.dto.ChargeRequest;
import br.com.oficina48.infrastructure.integration.mercadopago.dto.ChargeResponse;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoConcluidoEvent;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoFalhouEvent;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoPendenteEvent;
import br.com.oficina48.infrastructure.messaging.event.FalhaPagamentoEvent;
import br.com.oficina48.application.service.DocumentoStorage;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoSolicitadoEvent;
import br.com.oficina48.infrastructure.messaging.producer.FaturamentoProducer;
import br.com.oficina48.application.util.DocumentoConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class SolicitarFaturamentoUseCase {

    private static final Logger log = LoggerFactory.getLogger(SolicitarFaturamentoUseCase.class);

    private final FaturamentoRepository faturamentoRepository;
    private final BankProvider bankProvider;
    private final FaturamentoProducer faturamentoProducer;
    private final DocumentoStorage documentoStorage;

    public SolicitarFaturamentoUseCase(FaturamentoRepository faturamentoRepository,
                                       BankProvider bankProvider,
                                       FaturamentoProducer faturamentoProducer,
                                       DocumentoStorage documentoStorage) {
        this.faturamentoRepository = faturamentoRepository;
        this.bankProvider = bankProvider;
        this.faturamentoProducer = faturamentoProducer;
        this.documentoStorage = documentoStorage;
    }

    @Transactional
    public void executar(FaturamentoSolicitadoEvent event) {
        log.info("Iniciando processo de solicitação de faturamento para OS ID: {}", event.ordemServicoId());

            Optional<Faturamento> faturamentoExistente = faturamentoRepository
                .findFirstByOrdemServicoIdOrderByDataCriacaoDesc(event.ordemServicoId());

        if (faturamentoExistente.isPresent() && faturamentoExistente.get().getStatus() == FaturamentoStatus.CONCLUIDO) {
            log.warn("Faturamento para OS ID: {} já foi CONCLUÍDO. Reenviando evento de conclusão.", event.ordemServicoId());
            Faturamento f = faturamentoExistente.get();
            faturamentoProducer.enviarFaturamentoConcluido(new FaturamentoConcluidoEvent(
                    f.getOrdemServicoId(),
                    f.getSagaId(),
                    f.getPagamentoId(),
                    f.getValor()
            ));
            return;
        }

        try {
            ChargeRequest chargeRequest = ChargeRequest.builder()
                    .amount(event.valor())
                    .customerName(event.clienteNome())
                    .customerEmail(event.clienteEmail())
                    .customerCpf(event.clienteCpf())
                    .description(event.descricao() != null ? event.descricao() : "Faturamento da Ordem de Serviço " + event.ordemServicoId())
                    .externalReference(event.ordemServicoId().toString())
                    .build();

            log.info("Criando cobrança no Mercado Pago para OS ID: {}", event.ordemServicoId());
            ChargeResponse chargeResponse = bankProvider.createPixCharge(chargeRequest);

            Faturamento faturamento = faturamentoExistente.orElseGet(() -> Faturamento.builder()
                     .ordemServicoId(event.ordemServicoId())
                     .sagaId(event.sagaId())
                     .valor(event.valor())
                     .build());

            faturamento.setSagaId(event.sagaId());
            faturamento.setStatus(FaturamentoStatus.PENDENTE);
            faturamento.setPagamentoId(chargeResponse.getTransactionId());
            faturamento.setPagamentoLink(chargeResponse.getPaymentLink());

            faturamentoRepository.save(faturamento);
            log.info("Faturamento registrado como PENDENTE para OS ID: {}. Link de Pagamento: {}", 
                    event.ordemServicoId(), chargeResponse.getPaymentLink());

            // Gera e salva a solicitação de faturamento física
            String relatorio = gerarDocumentoFaturamento(event, faturamento);
            documentoStorage.salvar("links_pagamento", "faturamento_OS_" + event.ordemServicoId() + ".txt", relatorio);

            faturamentoProducer.enviarFaturamentoPendente(new FaturamentoPendenteEvent(
                    faturamento.getOrdemServicoId(),
                    faturamento.getSagaId(),
                    faturamento.getPagamentoId(),
                    faturamento.getPagamentoLink(),
                    faturamento.getValor()
            ));

        } catch (FalhaPagamentoException e) {
            log.error("Falha técnica ao gerar cobrança no Mercado Pago para OS ID: {}. Registrando faturamento como FALHOU.",
                    event.ordemServicoId(), e);

            registrarFaturamentoFalho(faturamentoExistente, event);

            String motivo = "Falha ao gerar cobrança no gateway de pagamento: " + e.getMessage();
            faturamentoProducer.enviarFaturamentoFalhou(new FaturamentoFalhouEvent(
                    event.ordemServicoId(),
                    event.sagaId(),
                    motivo,
                    event.valor()
            ));
            faturamentoProducer.enviarFalhaPagamento(new FalhaPagamentoEvent(
                    event.ordemServicoId(),
                    event.sagaId(),
                    event.valor(),
                    e.getProvider(),
                    e.getTipoErro(),
                    motivo
            ));
        } catch (Exception e) {
            log.error("Falha ao gerar cobrança no Mercado Pago para OS ID: {}. Registrando faturamento como FALHOU.",
                    event.ordemServicoId(), e);

            registrarFaturamentoFalho(faturamentoExistente, event);

            faturamentoProducer.enviarFaturamentoFalhou(new FaturamentoFalhouEvent(
                    event.ordemServicoId(),
                    event.sagaId(),
                    "Falha ao gerar cobrança no gateway de pagamento: " + e.getMessage(),
                    event.valor()
            ));
        }
    }

    private void registrarFaturamentoFalho(Optional<Faturamento> faturamentoExistente, FaturamentoSolicitadoEvent event) {
        Faturamento faturamento = faturamentoExistente.orElseGet(() -> Faturamento.builder()
                .ordemServicoId(event.ordemServicoId())
                .sagaId(event.sagaId())
                .valor(event.valor())
                .build());

        faturamento.setSagaId(event.sagaId());
        faturamento.setStatus(FaturamentoStatus.FALHOU);
        faturamentoRepository.save(faturamento);
    }

    private String gerarDocumentoFaturamento(FaturamentoSolicitadoEvent event, Faturamento faturamento) {
        StringBuilder sb = new StringBuilder();
        sb.append(DocumentoConstants.DIVIDER_HEADER);
        sb.append("         SOLICITAÇÃO DE PAGAMENTO       ").append(System.lineSeparator());
        sb.append(DocumentoConstants.DIVIDER_HEADER);
        sb.append(String.format("Id da ordem de serviço: %d%n", event.ordemServicoId()));
        sb.append(String.format("Id do faturamento: %d%n", faturamento.getId()));
        sb.append(String.format("Cliente: %s%n", event.clienteNome()));
        sb.append(String.format("E-mail: %s%n", event.clienteEmail()));
        sb.append(String.format("CPF: %s%n", event.clienteCpf()));
        sb.append(String.format("Valor Total: R$ %s%n", event.valor()));
        sb.append("Status: PENDENTE").append(System.lineSeparator());
        sb.append(DocumentoConstants.DIVIDER_SECTION);
        sb.append(String.format("ID do Pagamento (Mercado Pago): %s%n", faturamento.getPagamentoId()));
        sb.append(String.format("Link de Pagamento: %s%n", faturamento.getPagamentoLink()));
        sb.append(DocumentoConstants.DIVIDER_SECTION);
        sb.append(String.format("Data de Emissão: %s%n", java.time.LocalDateTime.now(DocumentoConstants.DEFAULT_ZONE_ID)));
        sb.append(DocumentoConstants.DIVIDER_HEADER);
        return sb.toString();
    }
}
