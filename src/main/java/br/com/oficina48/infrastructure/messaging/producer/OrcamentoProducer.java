package br.com.oficina48.infrastructure.messaging.producer;

import br.com.oficina48.infrastructure.messaging.event.OrcamentoAprovadoEvent;
import br.com.oficina48.infrastructure.messaging.event.OrcamentoReprovadoEvent;
import br.com.oficina48.infrastructure.properties.SqsProperties;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OrcamentoProducer {

    private static final Logger log = LoggerFactory.getLogger(OrcamentoProducer.class);

    private final SqsTemplate sqsTemplate;
    private final SqsProperties sqsProperties;

    public OrcamentoProducer(SqsTemplate sqsTemplate, SqsProperties sqsProperties) {
        this.sqsTemplate = sqsTemplate;
        this.sqsProperties = sqsProperties;
    }

    public void enviarOrcamentoAprovado(OrcamentoAprovadoEvent evento) {
        String queue = sqsProperties.queues().orcamentoAprovado();
        log.info("Enviando evento OrcamentoAprovadoEvent para fila SQS: {}. Payload: {}", queue, evento);
        sqsTemplate.send(queue, evento);
    }

    public void enviarOrcamentoReprovado(OrcamentoReprovadoEvent evento) {
        String queue = sqsProperties.queues().orcamentoReprovado();
        log.info("Enviando evento OrcamentoReprovadoEvent para fila SQS: {}. Payload: {}", queue, evento);
        sqsTemplate.send(queue, evento);
    }
}
