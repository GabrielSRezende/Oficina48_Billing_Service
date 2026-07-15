package br.com.oficina48.application.usecase;

import br.com.oficina48.domain.model.*;
import br.com.oficina48.domain.repository.OrcamentoRepository;
import br.com.oficina48.infrastructure.messaging.event.OrcamentoAprovadoEvent;
import br.com.oficina48.infrastructure.messaging.event.OrcamentoReprovadoEvent;
import br.com.oficina48.infrastructure.messaging.producer.OrcamentoProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessarDecisaoOrcamentoUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessarDecisaoOrcamentoUseCase.class);

    private final OrcamentoRepository orcamentoRepository;
    private final OrcamentoProducer orcamentoProducer;

    public ProcessarDecisaoOrcamentoUseCase(OrcamentoRepository orcamentoRepository,
                                            OrcamentoProducer orcamentoProducer) {
        this.orcamentoRepository = orcamentoRepository;
        this.orcamentoProducer = orcamentoProducer;
    }

    @Transactional
    public void executar(Long id, boolean aprovado) {
        log.info("Processando decisão do cliente para orçamento ID: {}. Aprovado: {}", id, aprovado);

        Orcamento orcamento = orcamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Orçamento não encontrado com o ID fornecido: " + id));

        if (orcamento.getStatus() != OrcamentoStatus.PENDENTE) {
            throw new IllegalStateException("Só é possível alterar a decisão de orçamentos no status PENDENTE. Status atual: " 
                    + orcamento.getStatus());
        }

        if (aprovado) {
            orcamento.setStatus(OrcamentoStatus.APROVADO);
            orcamentoRepository.save(orcamento);
            log.info("Orçamento ID: {} APROVADO. Enviando notificação para o OS Service.", id);
            orcamentoProducer.enviarOrcamentoAprovado(new OrcamentoAprovadoEvent(
                    orcamento.getOrdemServicoId(),
                    orcamento.getSagaId()
            ));
        } else {
            orcamento.setStatus(OrcamentoStatus.REPROVADO);
            orcamentoRepository.save(orcamento);
            log.info("Orçamento ID: {} REPROVADO. Enviando notificação para o OS Service.", id);
            orcamentoProducer.enviarOrcamentoReprovado(new OrcamentoReprovadoEvent(
                    orcamento.getOrdemServicoId(),
                    orcamento.getSagaId(),
                    MotivoErro.ORCAMENTO_REJEITADO_PELO_CLIENTE
            ));
        }
    }
}
