package br.com.oficina48.infrastructure.messaging.producer;

import br.com.oficina48.infrastructure.messaging.event.FaturamentoConcluidoEvent;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoFalhouEvent;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoPendenteEvent;
import br.com.oficina48.infrastructure.properties.SqsProperties;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FaturamentoProducer {

    private static final Logger log = LoggerFactory.getLogger(FaturamentoProducer.class);

    private final SqsTemplate sqsTemplate;
    private final SqsProperties sqsProperties;

    public FaturamentoProducer(SqsTemplate sqsTemplate, SqsProperties sqsProperties) {
        this.sqsTemplate = sqsTemplate;
        this.sqsProperties = sqsProperties;
    }

    public void enviarFaturamentoPendente(FaturamentoPendenteEvent evento) {
        String queue = sqsProperties.queues().faturamentoPendente();
        log.info("Enviando evento FaturamentoPendenteEvent para fila SQS: {}. Payload: {}", queue, evento);
        sqsTemplate.send(queue, evento);
    }

    public void enviarFaturamentoConcluido(FaturamentoConcluidoEvent evento) {
        String queue = sqsProperties.queues().faturamentoConcluido();
        log.info("Enviando evento FaturamentoConcluidoEvent para fila SQS: {}. Payload: {}", queue, evento);
        sqsTemplate.send(queue, evento);
    }

    public void enviarFaturamentoFalhou(FaturamentoFalhouEvent evento) {
        String queue = sqsProperties.queues().faturamentoFalhou();
        log.info("Enviando evento FaturamentoFalhouEvent para fila SQS: {}. Payload: {}", queue, evento);
        sqsTemplate.send(queue, evento);
    }
}
