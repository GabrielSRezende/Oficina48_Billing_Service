package br.com.oficina48.infrastructure.messaging.producer;

import br.com.oficina48.infrastructure.messaging.EventMapper;
import br.com.oficina48.infrastructure.messaging.event.*;
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
    private final EventMapper eventMapper;

    public FaturamentoProducer(
            SqsTemplate sqsTemplate,
            SqsProperties sqsProperties,
            EventMapper eventMapper
    ) {
        this.sqsTemplate = sqsTemplate;
        this.sqsProperties = sqsProperties;
        this.eventMapper = eventMapper;
    }

    public void enviarFaturamentoPendente(FaturamentoPendenteEvent evento) {
        String queue = sqsProperties.queues().faturamentoPendente();
        String json = this.eventMapper.toJson(evento);
        log.info("Enviando evento FaturamentoPendenteEvent para fila {}. Payload: {}", queue, json);
        this.send(queue, json);
    }

    public void enviarFaturamentoConcluido(FaturamentoConcluidoEvent evento) {
        String queue = sqsProperties.queues().faturamentoConcluido();
        String json = this.eventMapper.toJson(evento);
        log.info("Enviando evento FaturamentoConcluidoEvent para fila {}. Payload: {}", queue, json);
        this.send(queue, json);
    }

    public void enviarFaturamentoFalhou(FaturamentoFalhouEvent evento) {
        String queue = sqsProperties.queues().faturamentoFalhou();
        String json = this.eventMapper.toJson(evento);
        log.info("Enviando evento FaturamentoFalhouEvent para fila {}. Payload: {}", queue, json);
        this.send(queue, json);
    }

    public void enviarFalhaPagamento(FalhaPagamentoEvent evento) {
        String queue = sqsProperties.queues().falhaPagamento();
        String json = this.eventMapper.toJson(evento);
        log.info("Enviando evento FalhaPagamentoEvent para fila {}. Payload: {}", queue, json);
        this.send(queue, json);
    }

    private void send(String queue, String payload) {
        this.sqsTemplate.send(to -> to
                .queue(queue)
                .payload(payload));
    }

}
