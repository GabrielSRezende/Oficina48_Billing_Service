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
public class ChargeRequest {
    private BigDecimal amount;
    private String customerName;
    private String customerCpf;
    private String customerEmail;
    private String description;
    private String externalReference;
}
