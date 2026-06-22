package br.com.oficina48.application.usecase;

import br.com.oficina48.domain.model.Faturamento;
import br.com.oficina48.domain.model.FaturamentoStatus;
import br.com.oficina48.domain.repository.FaturamentoRepository;
import br.com.oficina48.infrastructure.integration.mercadopago.BankProvider;
import br.com.oficina48.infrastructure.integration.mercadopago.dto.ChargeRequest;
import br.com.oficina48.infrastructure.integration.mercadopago.dto.ChargeResponse;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoConcluidoEvent;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoFalhouEvent;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoSolicitadoEvent;
import br.com.oficina48.infrastructure.messaging.producer.FaturamentoProducer;
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

    public SolicitarFaturamentoUseCase(FaturamentoRepository faturamentoRepository,
                                       BankProvider bankProvider,
                                       FaturamentoProducer faturamentoProducer) {
        this.faturamentoRepository = faturamentoRepository;
        this.bankProvider = bankProvider;
        this.faturamentoProducer = faturamentoProducer;
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
                    .valor(event.valor())
                    .build());

            faturamento.setStatus(FaturamentoStatus.PENDENTE);
            faturamento.setPagamentoId(chargeResponse.getTransactionId());
            faturamento.setPagamentoLink(chargeResponse.getPaymentLink());

            faturamentoRepository.save(faturamento);
            log.info("Faturamento registrado como PENDENTE para OS ID: {}. Link de Pagamento: {}", 
                    event.ordemServicoId(), chargeResponse.getPaymentLink());

        } catch (Exception e) {
            log.error("Falha ao gerar cobrança no Mercado Pago para OS ID: {}. Registrando faturamento como FALHOU.", 
                    event.ordemServicoId(), e);

            Faturamento faturamento = faturamentoExistente.orElseGet(() -> Faturamento.builder()
                    .ordemServicoId(event.ordemServicoId())
                    .valor(event.valor())
                    .build());

            faturamento.setStatus(FaturamentoStatus.FALHOU);
            faturamentoRepository.save(faturamento);

            faturamentoProducer.enviarFaturamentoFalhou(new FaturamentoFalhouEvent(
                    event.ordemServicoId(),
                    "Falha ao gerar cobrança no gateway de pagamento: " + e.getMessage(),
                    event.valor()
            ));
        }
    }
}
