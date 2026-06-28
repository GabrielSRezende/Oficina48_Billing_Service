package br.com.oficina48.domain.repository;

import br.com.oficina48.domain.model.Faturamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FaturamentoRepository extends JpaRepository<Faturamento, Long> {
    Optional<Faturamento> findFirstByOrdemServicoIdOrderByDataCriacaoDesc(Long ordemServicoId);
    Optional<Faturamento> findByPagamentoId(String pagamentoId);
}
