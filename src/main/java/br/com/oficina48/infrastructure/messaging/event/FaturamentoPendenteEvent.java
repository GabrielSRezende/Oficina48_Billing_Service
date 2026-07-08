package br.com.oficina48.infrastructure.messaging.event;

import java.math.BigDecimal;

public record FaturamentoPendenteEvent(
        Long ordemServicoId,
        String sagaId,
        String pagamentoId,
        String pagamentoLink,
        BigDecimal valor
) {}
