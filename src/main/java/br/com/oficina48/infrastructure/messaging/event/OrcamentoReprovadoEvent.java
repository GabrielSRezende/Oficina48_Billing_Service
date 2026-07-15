package br.com.oficina48.infrastructure.messaging.event;

import br.com.oficina48.domain.model.MotivoErro;

public record OrcamentoReprovadoEvent(
        Long ordemServicoId,
        String sagaId,
        MotivoErro motivoErro
) {
}
