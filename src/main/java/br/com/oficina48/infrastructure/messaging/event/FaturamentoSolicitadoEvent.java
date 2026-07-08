package br.com.oficina48.infrastructure.messaging.event;

import java.math.BigDecimal;

public record FaturamentoSolicitadoEvent(
        Long ordemServicoId,
        String sagaId,
        BigDecimal valor,
        String clienteEmail,
        String clienteNome,
        String clienteCpf,
        String descricao
) {}
