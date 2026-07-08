package br.com.oficina48.infrastructure.messaging.consumer;

import br.com.oficina48.application.usecase.SolicitarFaturamentoUseCase;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoSolicitadoEvent;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FaturamentoSolicitadoConsumer {

    private static final Logger log = LoggerFactory.getLogger(FaturamentoSolicitadoConsumer.class);

    private final SolicitarFaturamentoUseCase solicitarFaturamentoUseCase;

    public FaturamentoSolicitadoConsumer(SolicitarFaturamentoUseCase solicitarFaturamentoUseCase) {
        this.solicitarFaturamentoUseCase = solicitarFaturamentoUseCase;
    }

    @SqsListener("${app.sqs.queues.faturamento-solicitado}${SUFIXO_LOCAL:}")
    public void receber(FaturamentoSolicitadoEvent evento) {
        log.info("Mensagem de solicitação de faturamento recebida. Ordem de Serviço ID: {}, Valor: {}", 
                evento.ordemServicoId(), evento.valor());
        try {
            solicitarFaturamentoUseCase.executar(evento);
        } catch (Exception e) {
            log.error("Erro ao processar solicitação de faturamento para OS ID: {}", evento.ordemServicoId(), e);
            // In a real SQS production environment, we could throw exception to trigger DLQ, or handle failing event publishing here.
            throw e;
        }
    }
}
