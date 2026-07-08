package br.com.oficina48.infrastructure.messaging.consumer;

import br.com.oficina48.application.usecase.SolicitarOrcamentoUseCase;
import br.com.oficina48.infrastructure.messaging.event.OrcamentoSolicitadoEvent;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.messaging.Message;

@Component
public class OrcamentoSolicitadoConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrcamentoSolicitadoConsumer.class);

    private final SolicitarOrcamentoUseCase solicitarOrcamentoUseCase;

    public OrcamentoSolicitadoConsumer(SolicitarOrcamentoUseCase solicitarOrcamentoUseCase) {
        this.solicitarOrcamentoUseCase = solicitarOrcamentoUseCase;
    }

    @SqsListener("${app.sqs.queues.orcamento-solicitado}${SUFIXO_LOCAL:}")
    public void receber(OrcamentoSolicitadoEvent evento) {
        log.info("Mensagem de solicitação de orçamento recebida. Ordem de Serviço ID: {}, Valor Total: {}", 
                evento.ordemServicoId(), evento.valorTotal());
        try {
            solicitarOrcamentoUseCase.executar(evento);
        } catch (Exception e) {
            log.error("Erro ao processar solicitação de orçamento para OS ID: {}", evento.ordemServicoId(), e);
            throw e;
        }
    }
}
