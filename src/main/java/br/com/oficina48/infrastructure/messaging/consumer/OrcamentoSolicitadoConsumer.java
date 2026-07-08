package br.com.oficina48.infrastructure.messaging.consumer;

import br.com.oficina48.application.usecase.SolicitarOrcamentoUseCase;
import br.com.oficina48.infrastructure.messaging.event.OrcamentoSolicitadoEvent;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrcamentoSolicitadoConsumer {

    private final SolicitarOrcamentoUseCase solicitarOrcamentoUseCase;

    public OrcamentoSolicitadoConsumer(SolicitarOrcamentoUseCase solicitarOrcamentoUseCase) {
        this.solicitarOrcamentoUseCase = solicitarOrcamentoUseCase;
    }

    @SqsListener("${app.sqs.queues.orcamento-solicitado:orcamento-solicitado}")
    public void receber(OrcamentoSolicitadoEvent evento) {
        log.info("\nMensagem recebida em: orcamento-solicitado. Mensagem: \n{}", evento);
        try {
            solicitarOrcamentoUseCase.executar(evento);
        } catch (Exception e) {
            log.error("Erro ao processar solicitação de orçamento para OS ID: {}", evento.ordemServicoId(), e);
            throw e;
        }
    }
}
