package br.com.oficina48.infrastructure.integration.mercadopago;

import br.com.oficina48.application.usecase.ConfirmarPagamentoFaturamentoUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/mercadopago")
public class MercadoPagoController {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoController.class);

    private final ConfirmarPagamentoFaturamentoUseCase confirmarPagamentoFaturamentoUseCase;

    public MercadoPagoController(ConfirmarPagamentoFaturamentoUseCase confirmarPagamentoFaturamentoUseCase) {
        this.confirmarPagamentoFaturamentoUseCase = confirmarPagamentoFaturamentoUseCase;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> receberNotificacao(@RequestBody Map<String, Object> payload) {
        log.info("Recebido webhook do Mercado Pago: {}", payload);

        try {
            String type = payload.get("type") != null ? String.valueOf(payload.get("type")) : null;
            String action = payload.get("action") != null ? String.valueOf(payload.get("action")) : null;
            String externalId = null;

            if (payload.containsKey("data") && payload.get("data") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) payload.get("data");
                if (data.containsKey("id")) {
                    externalId = String.valueOf(data.get("id"));
                }
            }

            if (externalId != null && ("payment".equals(type) || (action != null && action.startsWith("payment")))) {
                log.info("Processando webhook para o ID de Pagamento do Mercado Pago: {}", externalId);
                confirmarPagamentoFaturamentoUseCase.confirmarPorMercadoPagoId(externalId);
            } else {
                log.info("Webhook ignorado - Tipo: {}, Ação: {}, ID: {}", type, action, externalId);
            }

        } catch (Exception e) {
            log.error("Erro ao processar webhook do Mercado Pago: {}", e.getMessage(), e);
        }

        return ResponseEntity.ok("Recebido");
    }
}
