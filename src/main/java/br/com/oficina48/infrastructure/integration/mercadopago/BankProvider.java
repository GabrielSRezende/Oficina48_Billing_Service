package br.com.oficina48.infrastructure.integration.mercadopago;

import br.com.oficina48.infrastructure.integration.mercadopago.dto.ChargeRequest;
import br.com.oficina48.infrastructure.integration.mercadopago.dto.ChargeResponse;
import br.com.oficina48.infrastructure.integration.mercadopago.dto.ChargeStatus;

import java.util.Map;

public interface BankProvider {
    ChargeResponse createPixCharge(ChargeRequest request);
    ChargeStatus checkStatus(String transactionId, String externalReference);
    void handleWebhook(Map<String, Object> payload);
    String getProviderName();
}
