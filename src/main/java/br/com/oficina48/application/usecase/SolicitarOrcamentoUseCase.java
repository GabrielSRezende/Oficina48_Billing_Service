package br.com.oficina48.application.usecase;

import br.com.oficina48.domain.model.Orcamento;
import br.com.oficina48.domain.model.OrcamentoStatus;
import br.com.oficina48.domain.repository.OrcamentoRepository;
import br.com.oficina48.infrastructure.messaging.event.OrcamentoSolicitadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class SolicitarOrcamentoUseCase {

    private static final Logger log = LoggerFactory.getLogger(SolicitarOrcamentoUseCase.class);

    private final OrcamentoRepository orcamentoRepository;

    public SolicitarOrcamentoUseCase(OrcamentoRepository orcamentoRepository) {
        this.orcamentoRepository = orcamentoRepository;
    }

    @Transactional
    public void executar(OrcamentoSolicitadoEvent event) {
        log.info("Iniciando processo de solicitação de orçamento para OS ID: {}", event.ordemServicoId());

        Optional<Orcamento> orcamentoExistente = orcamentoRepository
                .findFirstByOrdemServicoIdOrderByDataCriacaoDesc(event.ordemServicoId());

        Orcamento orcamento = orcamentoExistente.orElseGet(() -> Orcamento.builder()
                .ordemServicoId(event.ordemServicoId())
                .build());

        orcamento.setValorTotal(event.valorTotal());
        orcamento.setStatus(OrcamentoStatus.PENDENTE);

        orcamentoRepository.save(orcamento);
        log.info("Orçamento registrado como PENDENTE para OS ID: {}, Valor Total: {}", 
                event.ordemServicoId(), event.valorTotal());

        // Gera o relatório textual simulado
        gerarRelatorioSimulado(event);
    }

    private void gerarRelatorioSimulado(OrcamentoSolicitadoEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("      RELATÓRIO DE ORÇAMENTO SIMULADO    \n");
        sb.append("========================================\n");
        sb.append(String.format("OS ID: %d\n", event.ordemServicoId()));
        sb.append(String.format("Placa do Veículo: %s\n", event.placaVeiculo()));
        sb.append(String.format("Valor Total: R$ %s\n", event.valorTotal()));
        sb.append("----------------------------------------\n");
        sb.append("Serviços:\n");
        if (event.servicos() != null) {
            event.servicos().forEach(s -> sb.append(String.format("  - %s: %s x R$ %s (Subtotal: R$ %s)\n",
                    s.nome(), s.quantidade(), s.valorUnitario(), s.subtotal())));
        }
        sb.append("Peças:\n");
        if (event.pecas() != null) {
            event.pecas().forEach(p -> sb.append(String.format("  - %s: %s x R$ %s (Subtotal: R$ %s)\n",
                    p.nome(), p.quantidade(), p.valorUnitario(), p.subtotal())));
        }
        sb.append("Insumos:\n");
        if (event.insumos() != null) {
            event.insumos().forEach(i -> sb.append(String.format("  - %s: %s x R$ %s (Subtotal: R$ %s)\n",
                    i.nome(), i.quantidade(), i.valorUnitario(), i.subtotal())));
        }
        sb.append("========================================\n");
        log.info("Relatório de orçamento simulado gerado com sucesso:\n{}", sb.toString());
    }
}
