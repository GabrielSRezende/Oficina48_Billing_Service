package br.com.oficina48.application.usecase;

import br.com.oficina48.domain.model.Faturamento;
import br.com.oficina48.domain.model.FaturamentoStatus;
import br.com.oficina48.domain.repository.FaturamentoRepository;
import br.com.oficina48.infrastructure.integration.mercadopago.BankProvider;
import br.com.oficina48.infrastructure.integration.mercadopago.dto.ChargeStatus;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoConcluidoEvent;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoFalhouEvent;
import br.com.oficina48.infrastructure.messaging.producer.FaturamentoProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ConfirmarPagamentoFaturamentoUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConfirmarPagamentoFaturamentoUseCase.class);

    private final FaturamentoRepository faturamentoRepository;
    private final BankProvider bankProvider;
    private final FaturamentoProducer faturamentoProducer;

    public ConfirmarPagamentoFaturamentoUseCase(FaturamentoRepository faturamentoRepository,
                                                BankProvider bankProvider,
                                                FaturamentoProducer faturamentoProducer) {
        this.faturamentoRepository = faturamentoRepository;
        this.bankProvider = bankProvider;
        this.faturamentoProducer = faturamentoProducer;
    }

    @Transactional
    public void confirmarPorMercadoPagoId(String paymentId) {
        log.info("Verificando status de pagamento no Mercado Pago para o ID: {}", paymentId);

        ChargeStatus chargeStatus = bankProvider.checkStatus(paymentId, null);
        log.info("Status retornado pelo Mercado Pago: {}", chargeStatus);

        Optional<Faturamento> faturamentoOpt = faturamentoRepository.findByPagamentoId(paymentId);

        if (faturamentoOpt.isEmpty() && chargeStatus.getExternalReference() != null) {
            try {
                Long osId = Long.parseLong(chargeStatus.getExternalReference());
                log.info("Faturamento não encontrado pelo ID de pagamento. Buscando pela Ordem de Serviço ID: {}", osId);
                faturamentoOpt = faturamentoRepository.findFirstByOrdemServicoIdOrderByDataCriacaoDesc(osId);
            } catch (NumberFormatException e) {
                log.warn("ExternalReference não é um ID de OS válido: {}", chargeStatus.getExternalReference());
            }
        }

        if (faturamentoOpt.isEmpty()) {
            log.error("Faturamento associado ao pagamento {} não foi encontrado no banco de dados.", paymentId);
            return;
        }

        Faturamento faturamento = faturamentoOpt.get();

        if (faturamento.getStatus() == FaturamentoStatus.CONCLUIDO) {
            log.info("Faturamento para a OS ID {} já está marcado como CONCLUÍDO.", faturamento.getOrdemServicoId());
            return;
        }

        if (chargeStatus.isPaid() || "approved".equalsIgnoreCase(chargeStatus.getStatus())) {
            log.info("Pagamento aprovado para a OS ID: {}. Atualizando faturamento para CONCLUÍDO.", faturamento.getOrdemServicoId());
            faturamento.setStatus(FaturamentoStatus.CONCLUIDO);
            faturamento.setPagamentoId(paymentId);
            faturamentoRepository.save(faturamento);

            faturamentoProducer.enviarFaturamentoConcluido(new FaturamentoConcluidoEvent(
                    faturamento.getOrdemServicoId(),
                    paymentId,
                    faturamento.getValor()
            ));

        } else if ("rejected".equalsIgnoreCase(chargeStatus.getStatus()) || 
                   "cancelled".equalsIgnoreCase(chargeStatus.getStatus()) || 
                   "refunded".equalsIgnoreCase(chargeStatus.getStatus()) ||
                   "charged_back".equalsIgnoreCase(chargeStatus.getStatus())) {
            
            log.info("Pagamento falhou (status: {}) para a OS ID: {}. Atualizando faturamento para FALHOU.", 
                    chargeStatus.getStatus(), faturamento.getOrdemServicoId());
            
            faturamento.setStatus(FaturamentoStatus.FALHOU);
            faturamentoRepository.save(faturamento);

            faturamentoProducer.enviarFaturamentoFalhou(new FaturamentoFalhouEvent(
                    faturamento.getOrdemServicoId(),
                    "Pagamento rejeitado ou cancelado no gateway: " + chargeStatus.getDetails(),
                    faturamento.getValor()
            ));
        } else {
            log.info("Pagamento para a OS ID: {} ainda está pendente (status: {}). Nenhuma ação necessária.", 
                    faturamento.getOrdemServicoId(), chargeStatus.getStatus());
        }
    }
}
