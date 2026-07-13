package br.com.oficina48.infrastructure.messaging.event;

import java.math.BigDecimal;

public record FalhaPagamentoEvent(
        Long ordemServicoId,
        String sagaId,
        BigDecimal valor,
        String provider,
        String tipoErro,
        String motivo
) {
}
