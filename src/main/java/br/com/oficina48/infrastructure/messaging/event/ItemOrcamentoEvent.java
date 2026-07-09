package br.com.oficina48.infrastructure.messaging.event;

import java.math.BigDecimal;

public record ItemOrcamentoEvent(
        String nome,
        Integer quantidade,
        BigDecimal preco,
        BigDecimal subtotal
) {

    @Override
    public BigDecimal subtotal() {
        return preco.multiply(new BigDecimal(quantidade));
    }
}
