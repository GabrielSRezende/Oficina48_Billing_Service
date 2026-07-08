package br.com.oficina48.infrastructure.messaging.consumer;

import br.com.oficina48.application.usecase.SolicitarFaturamentoUseCase;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoSolicitadoEvent;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FaturamentoSolicitadoConsumer {

    private final SolicitarFaturamentoUseCase solicitarFaturamentoUseCase;

    public FaturamentoSolicitadoConsumer(SolicitarFaturamentoUseCase solicitarFaturamentoUseCase) {
        this.solicitarFaturamentoUseCase = solicitarFaturamentoUseCase;
    }

    @SqsListener("${app.sqs.queues.faturamento-solicitado:faturamento-solicitado}")
    public void receber(FaturamentoSolicitadoEvent evento) {
        log.info("\nMensagem recebida em: faturamento-solicitado. Mensagem: \n{}", evento);
        try {
            solicitarFaturamentoUseCase.executar(evento);
        } catch (Exception e) {
            log.error("Erro ao processar solicitação de faturamento para OS ID: {}", evento.ordemServicoId(), e);
            // In a real SQS production environment, we could throw exception to trigger DLQ, or handle failing event publishing here.
            throw e;
        }
    }
}
