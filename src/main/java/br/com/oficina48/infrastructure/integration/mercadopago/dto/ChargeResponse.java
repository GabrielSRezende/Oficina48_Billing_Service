package br.com.oficina48.infrastructure.integration.mercadopago.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ChargeResponse {
    private String qrCode;
    private String qrCodeBase64;
    private String paymentLink;
    private String transactionId;
    private String status;
    private String externalReference;
}
