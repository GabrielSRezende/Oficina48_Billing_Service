package br.com.oficina48.bdd.steps;

import br.com.oficina48.domain.model.Orcamento;
import br.com.oficina48.domain.model.OrcamentoStatus;
import br.com.oficina48.domain.repository.OrcamentoRepository;
import br.com.oficina48.infrastructure.http.dto.OrcamentoDecisaoRequest;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrcamentoSteps {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrcamentoRepository orcamentoRepository;

    private Orcamento currentOrcamento;
    private ResponseEntity<Void> response;

    @Before
    public void cleanUp() {
        orcamentoRepository.deleteAll();
    }

    @Dado("que existe um orçamento cadastrado no status {string}")
    public void queExisteUmOrcamentoCadastrado(String statusStr) {
        OrcamentoStatus status = OrcamentoStatus.valueOf(statusStr.toUpperCase());
        Orcamento orcamento = Orcamento.builder()
                .ordemServicoId(100L)
                .sagaId("saga-100")
                .valorTotal(BigDecimal.valueOf(250.00))
                .status(status)
                .dataCriacao(LocalDateTime.now())
                .dataAtualizacao(LocalDateTime.now())
                .build();

        currentOrcamento = orcamentoRepository.save(orcamento);
    }

    @Quando("o cliente envia a decisão de aprovar o orçamento registrado")
    public void oClienteEnviaADecisaoDeAprovarOOrcamentoRegistrado() {
        enviarDecisao(currentOrcamento.getId(), true);
    }

    @Quando("o cliente envia a decisão de reprovar o orçamento registrado")
    public void oClienteEnviaADecisaoDeReprovarOOrcamentoRegistrado() {
        enviarDecisao(currentOrcamento.getId(), false);
    }

    private void enviarDecisao(Long id, boolean aprovado) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        OrcamentoDecisaoRequest request = new OrcamentoDecisaoRequest(aprovado);
        HttpEntity<OrcamentoDecisaoRequest> entity = new HttpEntity<>(request, headers);

        response = restTemplate.postForEntity("/orcamentos/" + id + "/decisao", entity, Void.class);
    }

    @Então("a resposta da requisição deve retornar status HTTP {int}")
    public void aRespostaDaRequisicaoDeveRetornarStatusHTTP(int statusCodeEsperado) {
        assertEquals(statusCodeEsperado, response.getStatusCode().value());
    }

    @Então("o orçamento registrado no banco de dados deve estar com o status {string}")
    public void oOrcamentoNoBancoDeDadosDeveEstarComOStatus(String statusEsperadoStr) {
        OrcamentoStatus statusEsperado = OrcamentoStatus.valueOf(statusEsperadoStr.toUpperCase());
        var orcamentoOpt = orcamentoRepository.findById(currentOrcamento.getId());

        assertTrue(orcamentoOpt.isPresent(), "Orçamento com ID " + currentOrcamento.getId() + " deveria existir no banco de dados.");
        assertEquals(statusEsperado, orcamentoOpt.get().getStatus());
    }
}
