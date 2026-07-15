package br.com.oficina48.domain.model;

public enum MotivoErro {

    ORDEM_SERVICO_NAO_ENCONTRADA("A ordem de serviço não foi encontrada."),
    SAGA_INVALIDA_OU_OBSOLETA("A transação da Saga está inválida ou expirou."),
    STATUS_INVALIDO_PARA_OPERACAO("A ordem de serviço não está no status correto para esta ação."),
    ORCAMENTO_REJEITADO_PELO_CLIENTE("Cliente rejeitou o diagnostico");


    private final String mensagem;

    MotivoErro(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getMensagem() {
        return this.mensagem;
    }
}