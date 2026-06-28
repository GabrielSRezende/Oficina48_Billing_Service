package br.com.oficina48.infrastructure.messaging.event;

import java.math.BigDecimal;

public record FaturamentoFalhouEvent(
        Long ordemServicoId,
        String motivo,
        BigDecimal valor
) {}
