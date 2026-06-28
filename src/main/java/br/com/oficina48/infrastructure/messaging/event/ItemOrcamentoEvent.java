package br.com.oficina48.infrastructure.messaging.event;

import java.math.BigDecimal;

public record ItemOrcamentoEvent(
        String nome,
        BigDecimal quantidade,
        BigDecimal valorUnitario,
        BigDecimal subtotal
) {
}
