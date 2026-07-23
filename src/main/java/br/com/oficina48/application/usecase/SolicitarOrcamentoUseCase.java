package br.com.oficina48.application.usecase;

import br.com.oficina48.application.service.DocumentoStorage;
import br.com.oficina48.application.util.DocumentoConstants;
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
    private final DocumentoStorage documentoStorage;

    public SolicitarOrcamentoUseCase(OrcamentoRepository orcamentoRepository, DocumentoStorage documentoStorage) {
        this.orcamentoRepository = orcamentoRepository;
        this.documentoStorage = documentoStorage;
    }

    @Transactional
    public void executar(OrcamentoSolicitadoEvent event) {
        log.info("Iniciando processo de solicitação de orçamento para OS ID: {}", event.ordemServicoId());

        Optional<Orcamento> orcamentoExistente = orcamentoRepository
                .findFirstByOrdemServicoIdOrderByDataCriacaoDesc(event.ordemServicoId());

        Orcamento orcamento;
        if (orcamentoExistente.isPresent()) {
            orcamento = orcamentoExistente.get();
            orcamento.setSagaId(event.sagaId());
            orcamento.setValorTotal(event.valorTotal());
            orcamento.setStatus(OrcamentoStatus.PENDENTE);
        } else {
            orcamento = Orcamento.builder()
                    .ordemServicoId(event.ordemServicoId())
                    .sagaId(event.sagaId())
                    .valorTotal(event.valorTotal())
                    .status(OrcamentoStatus.PENDENTE)
                    .build();
        }

        final var orcamentoSalvo = orcamentoRepository.save(orcamento);
        log.info("Orçamento registrado como PENDENTE para OS ID: {}, Valor Total: {}", 
                event.ordemServicoId(), event.valorTotal());

        // Gera e salva o relatório textual simulado
        String relatorio = gerarRelatorioSimulado(event, orcamentoSalvo.getId());
        documentoStorage.salvar("orcamentos", "orcamento_OS_" + event.ordemServicoId() + ".txt", relatorio);
    }

    private String gerarRelatorioSimulado(OrcamentoSolicitadoEvent event, Long idOrcamento) {
        StringBuilder sb = new StringBuilder();
        sb.append(DocumentoConstants.DIVIDER_HEADER);
        sb.append("      RELATÓRIO DE ORÇAMENTO SIMULADO    ").append(System.lineSeparator());
        sb.append(DocumentoConstants.DIVIDER_HEADER);
        sb.append(String.format("Id da ordem de serviço: %d%n", event.ordemServicoId()));
        sb.append(String.format("Id do orçamento: %d%n", idOrcamento));
        sb.append(String.format("Placa do Veículo: %s%n", event.placaVeiculo()));
        sb.append(String.format("Valor Total: R$ %s%n", event.valorTotal()));
        sb.append(DocumentoConstants.DIVIDER_SECTION);
        sb.append("Serviços:").append(System.lineSeparator());
        if (event.servicos() != null) {
            event.servicos().forEach(item -> sb.append(String.format(DocumentoConstants.FORMATO_ITEM_RELATORIO,
                     item.nome(), item.quantidade(), item.preco(), item.subtotal())));
        }
        sb.append("Peças:").append(System.lineSeparator());
        if (event.pecas() != null) {
            event.pecas().forEach(item -> sb.append(String.format(DocumentoConstants.FORMATO_ITEM_RELATORIO,
                     item.nome(), item.quantidade(), item.preco(), item.subtotal())));
        }
        sb.append("Insumos:").append(System.lineSeparator());
        if (event.insumos() != null) {
            event.insumos().forEach(item -> sb.append(String.format(DocumentoConstants.FORMATO_ITEM_RELATORIO,
                     item.nome(), item.quantidade(), item.preco(), item.subtotal())));
        }
        sb.append(DocumentoConstants.DIVIDER_HEADER);
        log.info("Relatório de orçamento simulado gerado com sucesso:{}{}", System.lineSeparator(), sb);
        return sb.toString();
    }
}
