package br.com.oficina48.infrastructure.integration.mercadopago.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ChargeStatus {
    private String status;
    private String details;
    private boolean paid;
    private String transactionId;
    private String externalReference;
    private BigDecimal netAmount;
    private BigDecimal totalPaidAmount;
}
