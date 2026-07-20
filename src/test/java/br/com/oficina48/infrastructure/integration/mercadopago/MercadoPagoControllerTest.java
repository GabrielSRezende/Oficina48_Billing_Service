package br.com.oficina48.infrastructure.integration.mercadopago;

import br.com.oficina48.application.usecase.ConfirmarPagamentoFaturamentoUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MercadoPagoControllerTest {

    private ConfirmarPagamentoFaturamentoUseCase confirmarUseCase;
    private MercadoPagoController controller;

    @BeforeEach
    void setUp() {
        confirmarUseCase = mock(ConfirmarPagamentoFaturamentoUseCase.class);
        controller = new MercadoPagoController(confirmarUseCase);
    }

    @Test
    @DisplayName("receberNotificacao - Com type=payment e data.id válido deve processar com sucesso")
    void deveProcessarWebhookComTypePayment() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "payment");

        Map<String, Object> data = new HashMap<>();
        data.put("id", "mp-12345");
        payload.put("data", data);

        ResponseEntity<String> response = controller.receberNotificacao(payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Recebido", response.getBody());
        verify(confirmarUseCase).confirmarPorMercadoPagoId("mp-12345");
    }

    @Test
    @DisplayName("receberNotificacao - Com action iniciando com payment e data.id válido deve processar com sucesso")
    void deveProcessarWebhookComActionPayment() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "payment.created");

        Map<String, Object> data = new HashMap<>();
        data.put("id", "mp-54321");
        payload.put("data", data);

        ResponseEntity<String> response = controller.receberNotificacao(payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Recebido", response.getBody());
        verify(confirmarUseCase).confirmarPorMercadoPagoId("mp-54321");
    }

    @Test
    @DisplayName("receberNotificacao - Com tipo ou ação diferente deve ignorar o processamento")
    void deveIgnorarWebhookComOutroTipo() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "subscription");

        Map<String, Object> data = new HashMap<>();
        data.put("id", "mp-sub-999");
        payload.put("data", data);

        ResponseEntity<String> response = controller.receberNotificacao(payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Recebido", response.getBody());
        verifyNoInteractions(confirmarUseCase);
    }

    @Test
    @DisplayName("receberNotificacao - Com payload vazio ou sem data.id deve ignorar")
    void deveIgnorarWebhookSemDataId() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "payment");
        // data.id ausente

        ResponseEntity<String> response = controller.receberNotificacao(payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Recebido", response.getBody());
        verifyNoInteractions(confirmarUseCase);
    }

    @Test
    @DisplayName("receberNotificacao - Caso useCase lance erro, deve capturar a exceção e retornar HTTP 200 OK")
    void deveRetornarOkMesmoSeOcorrerExcecaoNoUseCase() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "payment");

        Map<String, Object> data = new HashMap<>();
        data.put("id", "mp-error");
        payload.put("data", data);

        doThrow(new RuntimeException("Database offline")).when(confirmarUseCase).confirmarPorMercadoPagoId("mp-error");

        ResponseEntity<String> response = controller.receberNotificacao(payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Recebido", response.getBody());
        verify(confirmarUseCase).confirmarPorMercadoPagoId("mp-error");
    }
}
