package br.com.oficina48.infrastructure.messaging.producer;

import br.com.oficina48.infrastructure.messaging.event.FaturamentoConcluidoEvent;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoFalhouEvent;
import br.com.oficina48.infrastructure.messaging.event.FaturamentoPendenteEvent;
import br.com.oficina48.infrastructure.properties.SqsProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FaturamentoProducer {

    private static final Logger log = LoggerFactory.getLogger(FaturamentoProducer.class);

    private final SqsTemplate sqsTemplate;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;

    public FaturamentoProducer(
            SqsTemplate sqsTemplate,
            SqsProperties sqsProperties,
            ObjectMapper objectMapper
    ) {
        this.sqsTemplate = sqsTemplate;
        this.sqsProperties = sqsProperties;
        this.objectMapper = objectMapper;
    }

    public void enviarFaturamentoPendente(FaturamentoPendenteEvent evento) {
        String queue = sqsProperties.queues().faturamentoPendente();

        try {
            String json = objectMapper.writeValueAsString(evento);

            log.info("Enviando evento FaturamentoPendenteEvent para fila {}. Payload: {}", queue, json);

            sqsTemplate.send(to -> to
                    .queue(queue)
                    .payload(json)
            );

        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar FaturamentoPendenteEvent", e);
            throw new RuntimeException(e);
        }
    }

    public void enviarFaturamentoConcluido(FaturamentoConcluidoEvent evento) {
        String queue = sqsProperties.queues().faturamentoConcluido();

        try {
            String json = objectMapper.writeValueAsString(evento);

            log.info("Enviando evento FaturamentoConcluidoEvent para fila {}. Payload: {}", queue, json);

            sqsTemplate.send(to -> to
                    .queue(queue)
                    .payload(json)
            );

        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar FaturamentoConcluidoEvent", e);
            throw new RuntimeException(e);
        }
    }

    public void enviarFaturamentoFalhou(FaturamentoFalhouEvent evento) {
        String queue = sqsProperties.queues().faturamentoFalhou();

        try {
            String json = objectMapper.writeValueAsString(evento);

            log.info("Enviando evento FaturamentoFalhouEvent para fila {}. Payload: {}", queue, json);

            sqsTemplate.send(to -> to
                    .queue(queue)
                    .payload(json)
            );

        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar FaturamentoFalhouEvent", e);
            throw new RuntimeException(e);
        }
    }
}