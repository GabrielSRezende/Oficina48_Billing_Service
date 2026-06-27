package br.com.oficina48.infrastructure.http.dto;

import jakarta.validation.constraints.NotNull;

public record OrcamentoDecisaoRequest(
    @NotNull(message = "Aprovado não pode ser nulo")
    Boolean aprovado
) {}
