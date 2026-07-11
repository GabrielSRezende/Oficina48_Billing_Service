package br.com.oficina48.infrastructure.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalDocumentoStorageTest {

    @TempDir
    Path tempDir;

    private LocalDocumentoStorage localDocumentoStorage;

    @BeforeEach
    void setUp() {
        localDocumentoStorage = new LocalDocumentoStorage(tempDir.toString());
    }

    @Test
    @DisplayName("Deve salvar arquivo txt na subpasta correta e criar diretórios se não existirem")
    void deveSalvarArquivoComSucesso() throws IOException {
        String subpasta = "orcamentos";
        String nomeArquivo = "orcamento_OS_123.txt";
        String conteudo = "Conteudo de Teste do Orcamento";

        localDocumentoStorage.salvar(subpasta, nomeArquivo, conteudo);

        Path expectedFilePath = tempDir.resolve(subpasta).resolve(nomeArquivo);
        assertTrue(Files.exists(expectedFilePath), "O arquivo deveria ter sido criado fisicamente");
        assertEquals(conteudo, Files.readString(expectedFilePath), "O conteúdo do arquivo difere do esperado");
    }

    @Test
    @DisplayName("Deve salvar múltiplos arquivos em subpastas distintas")
    void deveSalvarMultiplosArquivosComSucesso() throws IOException {
        localDocumentoStorage.salvar("orcamentos", "orcamento_OS_1.txt", "Orcamento 1");
        localDocumentoStorage.salvar("links_pagamento", "faturamento_OS_2.txt", "Faturamento 2");

        Path orcamentoPath = tempDir.resolve("orcamentos").resolve("orcamento_OS_1.txt");
        Path faturamentoPath = tempDir.resolve("links_pagamento").resolve("faturamento_OS_2.txt");

        assertTrue(Files.exists(orcamentoPath));
        assertTrue(Files.exists(faturamentoPath));
        assertEquals("Orcamento 1", Files.readString(orcamentoPath));
        assertEquals("Faturamento 2", Files.readString(faturamentoPath));
    }

    @Test
    @DisplayName("Deve capturar IOException quando não puder criar o diretório da subpasta")
    void deveCapturarIOExceptionQuandoNaoPuderCriarDiretorio() throws IOException {
        Path regularFile = tempDir.resolve("subpasta_bloqueada");
        Files.writeString(regularFile, "bloqueio");

        assertDoesNotThrow(() -> {
            localDocumentoStorage.salvar("subpasta_bloqueada", "arquivo.txt", "conteudo");
        });

        Path targetFile = regularFile.resolve("arquivo.txt");
        assertFalse(Files.exists(targetFile));
    }
}
