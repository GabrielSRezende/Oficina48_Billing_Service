package br.com.oficina48.infrastructure.http.controller;

import br.com.oficina48.application.usecase.ProcessarDecisaoOrcamentoUseCase;
import br.com.oficina48.infrastructure.http.dto.OrcamentoDecisaoRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orcamentos")
public class OrcamentoController {

    private static final Logger log = LoggerFactory.getLogger(OrcamentoController.class);

    private final ProcessarDecisaoOrcamentoUseCase processarDecisaoOrcamentoUseCase;

    public OrcamentoController(ProcessarDecisaoOrcamentoUseCase processarDecisaoOrcamentoUseCase) {
        this.processarDecisaoOrcamentoUseCase = processarDecisaoOrcamentoUseCase;
    }

    @PostMapping("/{id}/decisao")
    public ResponseEntity<Void> processarDecisao(
            @PathVariable Long id,
            @Valid @RequestBody OrcamentoDecisaoRequest request) {
        log.info("Recebida requisição HTTP de decisão do cliente para Orçamento ID: {}. Decisão: {}", id, request.aprovado());
        processarDecisaoOrcamentoUseCase.executar(id, request.aprovado());
        return ResponseEntity.noContent().build();
    }
}
