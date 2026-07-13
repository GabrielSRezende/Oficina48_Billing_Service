package br.com.oficina48.infrastructure.integration.mercadopago;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.net.MPResultsResourcesPage;
import com.mercadopago.net.MPSearchRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import br.com.oficina48.infrastructure.integration.mercadopago.dto.ChargeRequest;
import br.com.oficina48.infrastructure.integration.mercadopago.dto.ChargeResponse;
import br.com.oficina48.infrastructure.integration.mercadopago.dto.ChargeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
public class MercadoPagoProvider implements BankProvider {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoProvider.class);

    @Value("${mercadopago.access-key}")
    private String accessToken;

    @Value("${mercadopago.webhook-url}")
    private String notificationUrl;

    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    public String getProviderName() {
        return "MRCPAGO";
    }

    @Override
    public ChargeResponse createPixCharge(ChargeRequest request) {
        try {
            MercadoPagoConfig.setAccessToken(accessToken);

            PreferenceClient client = new PreferenceClient();

            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .id(request.getExternalReference())
                    .title(request.getDescription())
                    .quantity(1)
                    .description(request.getDescription())
                    .categoryId("servicos")
                    .unitPrice(request.getAmount())
                    .currencyId("BRL")
                    .build();

            String formattedBaseUrl = baseUrl;
            if (!formattedBaseUrl.startsWith("http://") && !formattedBaseUrl.startsWith("https://")) {
                formattedBaseUrl = "https://" + formattedBaseUrl;
            }

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(formattedBaseUrl + "/sucesso")
                    .failure(formattedBaseUrl + "/falha")
                    .pending(formattedBaseUrl + "/pendente")
                    .build();

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(Arrays.asList(item))
                    .payer(PreferencePayerRequest.builder()
                            .email(request.getCustomerEmail())
                            .name(request.getCustomerName())
                            .build())
                    .backUrls(backUrls)
                    .autoReturn("approved")
                    .notificationUrl(notificationUrl)
                    .externalReference(request.getExternalReference())
                    .build();

            log.info("Generating Mercado Pago preference. Notification URL: {}", notificationUrl);
            Preference preference = client.create(preferenceRequest);

            return ChargeResponse.builder()
                    .paymentLink(preference.getInitPoint())
                    .transactionId(preference.getId())
                    .externalReference(request.getExternalReference())
                    .status("pending")
                    .build();

        } catch (MPApiException e) {
            String errorMsg = String.format("Erro na API do Mercado Pago (Status %d): %s",
                    e.getApiResponse().getStatusCode(), e.getApiResponse().getContent());
            log.error(errorMsg);
            throw new FalhaPagamentoException(getProviderName(), "MP_API_EXCEPTION", errorMsg, e);
        } catch (MPException e) {
            String errorMsg = "Erro na SDK do Mercado Pago: " + e.getMessage();
            log.error(errorMsg, e);
            throw new FalhaPagamentoException(getProviderName(), "MP_EXCEPTION", errorMsg, e);
        }
    }

    @Override
    public ChargeStatus checkStatus(String transactionId, String externalReference) {
        try {
            MercadoPagoConfig.setAccessToken(accessToken);
            PaymentClient client = new PaymentClient();

            // 1. Prioritize searching by external_reference
            if (externalReference != null && !externalReference.isEmpty()) {
                Map<String, Object> filters = new HashMap<>();
                filters.put("external_reference", externalReference);

                MPSearchRequest searchRequest = MPSearchRequest.builder()
                        .limit(1)
                        .offset(0)
                        .filters(filters)
                        .build();

                MPResultsResourcesPage<Payment> results = client.search(searchRequest);

                if (results.getResults() != null && !results.getResults().isEmpty()) {
                    Payment payment = results.getResults().get(0);
                    log.debug("Found payment via external_reference: {}", payment.getId());
                    return mapToChargeStatus(payment);
                }
            }

            // 2. Fallback to direct ID fetch if search returned nothing
            if (transactionId != null && transactionId.matches("\\d+")) {
                Payment payment = client.get(Long.valueOf(transactionId));
                return mapToChargeStatus(payment);
            }

            return ChargeStatus.builder()
                    .status("pending")
                    .details("Waiting for payment selection")
                    .paid(false)
                    .externalReference(externalReference)
                    .transactionId(transactionId)
                    .build();

        } catch (MPException | MPApiException e) {
            log.error("Mercado Pago status check failed: {}", e.getMessage());
            return ChargeStatus.builder()
                    .status("error")
                    .details(e.getMessage())
                    .build();
        }
    }

    private ChargeStatus mapToChargeStatus(Payment payment) {
        return ChargeStatus.builder()
                .status(payment.getStatus())
                .paid("approved".equalsIgnoreCase(payment.getStatus()))
                .details(payment.getStatusDetail())
                .externalReference(payment.getExternalReference())
                .transactionId(payment.getId().toString())
                .netAmount(
                        payment.getTransactionDetails() != null ? payment.getTransactionDetails().getNetReceivedAmount()
                                : null)
                .totalPaidAmount(payment.getTransactionDetails() != null
                        ? payment.getTransactionDetails().getTotalPaidAmount()
                        : null)
                .build();
    }

    @Override
    public void handleWebhook(Map<String, Object> payload) {
        log.info("Processing Mercado Pago Webhook: {}", payload);
        // Handled directly inside UseCase / Controller to integrate with Saga
    }
}
