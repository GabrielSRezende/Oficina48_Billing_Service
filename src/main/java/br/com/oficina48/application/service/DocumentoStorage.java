package br.com.oficina48.application.service;

public interface DocumentoStorage {
    void salvar(String subpasta, String nomeArquivo, String conteudo);
}
