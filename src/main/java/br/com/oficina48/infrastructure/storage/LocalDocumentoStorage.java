package br.com.oficina48.infrastructure.storage;

import br.com.oficina48.application.service.DocumentoStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class LocalDocumentoStorage implements DocumentoStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalDocumentoStorage.class);

    private final Path storageDirectory;

    public LocalDocumentoStorage(@Value("${app.documentos.path:./documentos}") String path) {
        this.storageDirectory = Paths.get(path).toAbsolutePath().normalize();
        log.info("Diretório base de documentos configurado em: {}", this.storageDirectory);
    }

    @Override
    public void salvar(String subpasta, String nomeArquivo, String conteudo) {
        try {
            Path targetDir = storageDirectory.resolve(subpasta);
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(nomeArquivo);
            Files.writeString(targetFile, conteudo);
            log.info("Documento [{}] de [{}] salvo com sucesso em: {}", nomeArquivo, subpasta, targetFile);
        } catch (IOException e) {
            log.error("Erro ao salvar documento [{}] na subpasta [{}]: {}", nomeArquivo, subpasta, e.getMessage(), e);
        }
    }
}
