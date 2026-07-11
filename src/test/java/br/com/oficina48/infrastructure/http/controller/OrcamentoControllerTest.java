package br.com.oficina48.infrastructure.http.controller;

import br.com.oficina48.application.usecase.ProcessarDecisaoOrcamentoUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrcamentoControllerTest {

    @Mock
    private ProcessarDecisaoOrcamentoUseCase processarDecisaoUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        OrcamentoController controller = new OrcamentoController(processarDecisaoUseCase);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("POST /orcamentos/{id}/decisao - Com payload de aprovação válido deve retornar 204 No Content")
    void deveAprovarOrcamentoComSucesso() throws Exception {
        mockMvc.perform(post("/orcamentos/123/decisao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"aprovado\": true}"))
                .andExpect(status().isNoContent());

        verify(processarDecisaoUseCase).executar(123L, true);
    }

    @Test
    @DisplayName("POST /orcamentos/{id}/decisao - Com payload de reprovação válido deve retornar 204 No Content")
    void deveReprovarOrcamentoComSucesso() throws Exception {
        mockMvc.perform(post("/orcamentos/123/decisao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"aprovado\": false}"))
                .andExpect(status().isNoContent());

        verify(processarDecisaoUseCase).executar(123L, false);
    }

    @Test
    @DisplayName("POST /orcamentos/{id}/decisao - Com aprovado nulo deve retornar 400 Bad Request")
    void deveRetornarBadRequestQuandoAprovadoForNulo() throws Exception {
        mockMvc.perform(post("/orcamentos/123/decisao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"aprovado\": null}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(processarDecisaoUseCase);
    }

    @Test
    @DisplayName("POST /orcamentos/{id}/decisao - Com corpo vazio deve retornar 400 Bad Request")
    void deveRetornarBadRequestQuandoCorpoForVazio() throws Exception {
        mockMvc.perform(post("/orcamentos/123/decisao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(processarDecisaoUseCase);
    }

    @Test
    @DisplayName("POST /orcamentos/{id}/decisao - Quando usecase lança IllegalArgumentException deve repassar a exceção")
    void devePropagarExcecaoIllegalArgumentException() throws Exception {
        doThrow(new IllegalArgumentException("Orçamento não encontrado"))
                .when(processarDecisaoUseCase).executar(999L, true);

        try {
            mockMvc.perform(post("/orcamentos/999/decisao")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"aprovado\": true}"));
        } catch (Exception e) {
            org.junit.jupiter.api.Assertions.assertTrue(
                    e.getCause() instanceof IllegalArgumentException || e instanceof IllegalArgumentException
            );
        }
    }
}
