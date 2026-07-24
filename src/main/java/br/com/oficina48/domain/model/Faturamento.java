package br.com.oficina48.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "tb_faturamento")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Faturamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ordem_servico_id", nullable = false)
    private Long ordemServicoId;

    @Column(name = "saga_id", length = 100)
    private String sagaId;

    @Column(name = "valor", nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FaturamentoStatus status;

    @Column(name = "pagamento_id", length = 100)
    private String pagamentoId;

    @Column(name = "pagamento_link", length = 500)
    private String pagamentoLink;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    private static final ZoneId ZONE_ID = ZoneId.of("America/Sao_Paulo");

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now(ZONE_ID);
        dataAtualizacao = LocalDateTime.now(ZONE_ID);
    }

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now(ZONE_ID);
    }
}
