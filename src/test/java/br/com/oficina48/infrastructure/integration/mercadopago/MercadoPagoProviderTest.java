package br.com.oficina48.infrastructure.integration.mercadopago;

import br.com.oficina48.infrastructure.integration.mercadopago.dto.ChargeRequest;
import br.com.oficina48.infrastructure.integration.mercadopago.dto.ChargeResponse;
import br.com.oficina48.infrastructure.integration.mercadopago.dto.ChargeStatus;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.net.MPResponse;
import com.mercadopago.net.MPResultsResourcesPage;
import com.mercadopago.net.MPSearchRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.payment.PaymentTransactionDetails;
import com.mercadopago.resources.preference.Preference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MercadoPagoProviderTest {

    private MercadoPagoProvider provider;

    @BeforeEach
    void setUp() {
        provider = new MercadoPagoProvider();
        ReflectionTestUtils.setField(provider, "accessToken", "test-token");
        ReflectionTestUtils.setField(provider, "notificationUrl", "http://webhook.url");
        ReflectionTestUtils.setField(provider, "baseUrl", "http://base.url");
    }

    @Test
    @DisplayName("getProviderName - Deve retornar MRCPAGO")
    void deveRetornarNomeDoProvedor() {
        assertEquals("MRCPAGO", provider.getProviderName());
    }

    @Test
    @DisplayName("createPixCharge - Deve criar cobrança Pix com sucesso")
    void deveCriarPixChargeComSucesso() {
        ChargeRequest request = ChargeRequest.builder()
                .externalReference("ext-123")
                .description("Descrição do teste")
                .amount(BigDecimal.TEN)
                .customerEmail("cliente@teste.com")
                .customerName("Cliente Teste")
                .build();

        Preference mockPreference = mock(Preference.class);
        when(mockPreference.getInitPoint()).thenReturn("http://init.point");
        when(mockPreference.getId()).thenReturn("pref-123");

        try (MockedStatic<MercadoPagoConfig> configMock = mockStatic(MercadoPagoConfig.class);
             MockedConstruction<PreferenceClient> constructionMock = mockConstruction(PreferenceClient.class,
                     (mock, context) -> when(mock.create(any(PreferenceRequest.class))).thenReturn(mockPreference))) {

            ChargeResponse response = provider.createPixCharge(request);

            assertNotNull(response);
            assertEquals("http://init.point", response.getPaymentLink());
            assertEquals("pref-123", response.getTransactionId());
            assertEquals("ext-123", response.getExternalReference());
            assertEquals("pending", response.getStatus());

            configMock.verify(() -> MercadoPagoConfig.setAccessToken("test-token"));
            assertEquals(1, constructionMock.constructed().size());
        }
    }

    @Test
    @DisplayName("createPixCharge - Tratamento de erro MPApiException")
    void deveLancarExcecaoQuandoMPApiFalhar() {
        ChargeRequest request = ChargeRequest.builder()
                .externalReference("ext-123")
                .amount(BigDecimal.TEN)
                .build();

        MPResponse mockResponse = mock(MPResponse.class);
        when(mockResponse.getStatusCode()).thenReturn(400);
        when(mockResponse.getContent()).thenReturn("Bad Request Error");

        MPApiException apiException = mock(MPApiException.class);
        when(apiException.getApiResponse()).thenReturn(mockResponse);

        try (MockedStatic<MercadoPagoConfig> configMock = mockStatic(MercadoPagoConfig.class);
             MockedConstruction<PreferenceClient> constructionMock = mockConstruction(PreferenceClient.class,
                     (mock, context) -> when(mock.create(any(PreferenceRequest.class))).thenThrow(apiException))) {

            RuntimeException exception = assertThrows(RuntimeException.class, () -> provider.createPixCharge(request));
            assertTrue(exception.getMessage().contains("Erro na API do Mercado Pago (Status 400): Bad Request Error"));
        }
    }

    @Test
    @DisplayName("createPixCharge - Tratamento de erro MPException")
    void deveLancarExcecaoQuandoSDKFalhar() {
        ChargeRequest request = ChargeRequest.builder()
                .externalReference("ext-123")
                .amount(BigDecimal.TEN)
                .build();

        MPException mpException = new MPException("Network error");

        try (MockedStatic<MercadoPagoConfig> configMock = mockStatic(MercadoPagoConfig.class);
             MockedConstruction<PreferenceClient> constructionMock = mockConstruction(PreferenceClient.class,
                     (mock, context) -> when(mock.create(any(PreferenceRequest.class))).thenThrow(mpException))) {

            RuntimeException exception = assertThrows(RuntimeException.class, () -> provider.createPixCharge(request));
            assertTrue(exception.getMessage().contains("Erro na SDK do Mercado Pago: Network error"));
        }
    }

    @Test
    @DisplayName("checkStatus - Deve retornar status consultado por externalReference com sucesso")
    void deveConsultarStatusPorExternalReference() {
        Payment mockPayment = mock(Payment.class);
        when(mockPayment.getStatus()).thenReturn("approved");
        when(mockPayment.getStatusDetail()).thenReturn("accredited");
        when(mockPayment.getExternalReference()).thenReturn("ext-123");
        when(mockPayment.getId()).thenReturn(98765L);

        PaymentTransactionDetails mockDetails = mock(PaymentTransactionDetails.class);
        when(mockDetails.getNetReceivedAmount()).thenReturn(BigDecimal.valueOf(9.5));
        when(mockDetails.getTotalPaidAmount()).thenReturn(BigDecimal.TEN);
        when(mockPayment.getTransactionDetails()).thenReturn(mockDetails);

        MPResultsResourcesPage<Payment> mockPage = mock(MPResultsResourcesPage.class);
        when(mockPage.getResults()).thenReturn(Collections.singletonList(mockPayment));

        try (MockedStatic<MercadoPagoConfig> configMock = mockStatic(MercadoPagoConfig.class);
             MockedConstruction<PaymentClient> constructionMock = mockConstruction(PaymentClient.class,
                     (mock, context) -> when(mock.search(any(MPSearchRequest.class))).thenReturn(mockPage))) {

            ChargeStatus status = provider.checkStatus("mp-123", "ext-123");

            assertNotNull(status);
            assertEquals("approved", status.getStatus());
            assertTrue(status.isPaid());
            assertEquals("accredited", status.getDetails());
            assertEquals("ext-123", status.getExternalReference());
            assertEquals("98765", status.getTransactionId());
            assertEquals(BigDecimal.valueOf(9.5), status.getNetAmount());
            assertEquals(BigDecimal.TEN, status.getTotalPaidAmount());
        }
    }

    @Test
    @DisplayName("checkStatus - Deve usar fallback por ID se pesquisa por externalReference retornar vazio")
    void deveConsultarStatusPorIdFallback() {
        Payment mockPayment = mock(Payment.class);
        when(mockPayment.getStatus()).thenReturn("pending");
        when(mockPayment.getStatusDetail()).thenReturn("pending_waiting_transfer");
        when(mockPayment.getExternalReference()).thenReturn("ext-123");
        when(mockPayment.getId()).thenReturn(12345L);
        when(mockPayment.getTransactionDetails()).thenReturn(null);

        MPResultsResourcesPage<Payment> mockPageVazia = mock(MPResultsResourcesPage.class);
        when(mockPageVazia.getResults()).thenReturn(Collections.emptyList());

        try (MockedStatic<MercadoPagoConfig> configMock = mockStatic(MercadoPagoConfig.class);
             MockedConstruction<PaymentClient> constructionMock = mockConstruction(PaymentClient.class,
                     (mock, context) -> {
                         when(mock.search(any(MPSearchRequest.class))).thenReturn(mockPageVazia);
                         when(mock.get(12345L)).thenReturn(mockPayment);
                     })) {

            ChargeStatus status = provider.checkStatus("12345", "ext-123");

            assertNotNull(status);
            assertEquals("pending", status.getStatus());
            assertFalse(status.isPaid());
            assertEquals("12345", status.getTransactionId());
            assertNull(status.getNetAmount());
        }
    }

    @Test
    @DisplayName("checkStatus - Deve retornar status pendente se ID for inválido")
    void deveRetornarPendenteSeNaoEncontrarPorIdOuRef() {
        MPResultsResourcesPage<Payment> mockPageVazia = mock(MPResultsResourcesPage.class);
        when(mockPageVazia.getResults()).thenReturn(Collections.emptyList());

        try (MockedStatic<MercadoPagoConfig> configMock = mockStatic(MercadoPagoConfig.class);
             MockedConstruction<PaymentClient> constructionMock = mockConstruction(PaymentClient.class,
                     (mock, context) -> when(mock.search(any(MPSearchRequest.class))).thenReturn(mockPageVazia))) {

            ChargeStatus status = provider.checkStatus("invalid-id", "ext-123");

            assertNotNull(status);
            assertEquals("pending", status.getStatus());
            assertEquals("Waiting for payment selection", status.getDetails());
            assertFalse(status.isPaid());
        }
    }

    @Test
    @DisplayName("checkStatus - Tratamento de exceção na busca")
    void deveRetornarStatusErroSeSDKLancarExcecao() {
        try (MockedStatic<MercadoPagoConfig> configMock = mockStatic(MercadoPagoConfig.class);
             MockedConstruction<PaymentClient> constructionMock = mockConstruction(PaymentClient.class,
                     (mock, context) -> when(mock.search(any(MPSearchRequest.class))).thenThrow(new MPException("Connection failed")))) {

            ChargeStatus status = provider.checkStatus("123", "ext-123");

            assertNotNull(status);
            assertEquals("error", status.getStatus());
            assertEquals("Connection failed", status.getDetails());
        }
    }

    @Test
    @DisplayName("handleWebhook - Sem comportamento definido")
    void deveExecutarWebhookSemLancarExcecao() {
        assertDoesNotThrow(() -> provider.handleWebhook(Collections.emptyMap()));
    }
}
