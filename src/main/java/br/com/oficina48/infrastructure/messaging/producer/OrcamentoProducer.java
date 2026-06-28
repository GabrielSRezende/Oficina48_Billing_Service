package br.com.oficina48.infrastructure.messaging.producer;

import br.com.oficina48.infrastructure.messaging.event.OrcamentoAprovadoEvent;
import br.com.oficina48.infrastructure.messaging.event.OrcamentoReprovadoEvent;
import br.com.oficina48.infrastructure.properties.SqsProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OrcamentoProducer {

    private static final Logger log = LoggerFactory.getLogger(OrcamentoProducer.class);

    private final SqsTemplate sqsTemplate;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;

    public OrcamentoProducer(
            SqsTemplate sqsTemplate,
            SqsProperties sqsProperties,
            ObjectMapper objectMapper
    ) {
        this.sqsTemplate = sqsTemplate;
        this.sqsProperties = sqsProperties;
        this.objectMapper = objectMapper;
    }

    public void enviarOrcamentoAprovado(OrcamentoAprovadoEvent evento) {
        String queue = sqsProperties.queues().orcamentoAprovado();

        try {
            String json = objectMapper.writeValueAsString(evento);

            log.info("Enviando evento OrcamentoAprovadoEvent para fila {}. Payload: {}", queue, json);

            sqsTemplate.send(to -> to
                    .queue(queue)
                    .payload(json)
            );

        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar OrcamentoAprovadoEvent", e);
            throw new RuntimeException(e);
        }
    }

    public void enviarOrcamentoReprovado(OrcamentoReprovadoEvent evento) {
        String queue = sqsProperties.queues().orcamentoReprovado();

        try {
            String json = objectMapper.writeValueAsString(evento);

            log.info("Enviando evento OrcamentoReprovadoEvent para fila {}. Payload: {}", queue, json);

            sqsTemplate.send(to -> to
                    .queue(queue)
                    .payload(json)
            );

        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar OrcamentoReprovadoEvent", e);
            throw new RuntimeException(e);
        }
    }
}