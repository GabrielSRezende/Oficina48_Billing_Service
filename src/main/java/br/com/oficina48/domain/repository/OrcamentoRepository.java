package br.com.oficina48.domain.repository;

import br.com.oficina48.domain.model.Orcamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {
    Optional<Orcamento> findFirstByOrdemServicoIdOrderByDataCriacaoDesc(Long ordemServicoId);
}
