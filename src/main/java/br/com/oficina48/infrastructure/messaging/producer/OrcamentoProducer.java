package br.com.oficina48.infrastructure.messaging.producer;

import br.com.oficina48.infrastructure.messaging.EventMapper;
import br.com.oficina48.infrastructure.messaging.event.OrcamentoAprovadoEvent;
import br.com.oficina48.infrastructure.messaging.event.OrcamentoReprovadoEvent;
import br.com.oficina48.infrastructure.properties.SqsProperties;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrcamentoProducer {

    private final SqsTemplate sqsTemplate;
    private final SqsProperties sqsProperties;
    private final EventMapper eventMapper;

    public OrcamentoProducer(
            SqsTemplate sqsTemplate,
            SqsProperties sqsProperties,
            EventMapper eventMapper
    ) {
        this.sqsTemplate = sqsTemplate;
        this.sqsProperties = sqsProperties;
        this.eventMapper = eventMapper;
    }

    public void enviarOrcamentoAprovado(OrcamentoAprovadoEvent evento) {
        String queue = sqsProperties.queues().orcamentoAprovado();
        String json = this.eventMapper.toJson(evento);
        log.info("Enviando evento OrcamentoAprovadoEvent para fila {}. Payload: {}", queue, json);
        this.send(queue, json);
    }

    public void enviarOrcamentoReprovado(OrcamentoReprovadoEvent evento) {
        String queue = sqsProperties.queues().orcamentoReprovado();
        String json = this.eventMapper.toJson(evento);
        log.info("Enviando evento OrcamentoReprovadoEvent para fila {}. Payload: {}", queue, json);
        this.send(queue, json);
    }

    private void send(String queue, String payload) {
        this.sqsTemplate.send(to -> to
                .queue(queue)
                .payload(payload));
    }

}