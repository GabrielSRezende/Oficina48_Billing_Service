package br.com.oficina48.infrastructure.messaging.event;

import java.math.BigDecimal;

public record FaturamentoFalhouEvent(
        Long ordemServicoId,
        String sagaId,
        String motivo,
        BigDecimal valor
) {}
