package br.com.oficina48.infrastructure.messaging.event;

import java.math.BigDecimal;

public record FaturamentoConcluidoEvent(
        Long ordemServicoId,
        String pagamentoId,
        BigDecimal valor
) {}
