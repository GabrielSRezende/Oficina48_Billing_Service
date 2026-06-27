package br.com.oficina48.infrastructure.messaging.event;

import java.math.BigDecimal;
import java.util.List;

public record OrcamentoSolicitadoEvent(
        Long ordemServicoId,
        Long veiculoId,
        String placaVeiculo,
        List<ItemOrcamentoEvent> servicos,
        List<ItemOrcamentoEvent> pecas,
        List<ItemOrcamentoEvent> insumos,
        BigDecimal valorTotal
) {
}
