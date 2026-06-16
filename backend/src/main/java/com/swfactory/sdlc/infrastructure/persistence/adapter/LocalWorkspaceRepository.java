package com.swfactory.sdlc.infrastructure.persistence.adapter;

import com.swfactory.sdlc.domain.repository.WorkspaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Adaptador de Infraestructura. Implementa WorkspaceRepository interactuando directamente
 * con el sistema de archivos del host local para escribir código y ejecutar la compilación.
 */
@Component
public class LocalWorkspaceRepository implements WorkspaceRepository {

    private static final Logger log = LoggerFactory.getLogger(LocalWorkspaceRepository.class);

    private final Path workspaceRoot;

    public LocalWorkspaceRepository(@Value("${app.workspace.root:/home/tino/Projects/AI-Agents-System}") String workspaceRootPath) {
        this.workspaceRoot = Paths.get(workspaceRootPath);
    }

    @Override
    public void writeFile(String relativePath, String content) {
        Path targetPath = workspaceRoot.resolve(relativePath);
        try {
            Files.createDirectories(targetPath.getParent());
            Files.writeString(targetPath, content);
            log.info("Archivo escrito en el espacio de trabajo: {}", targetPath);
        } catch (IOException e) {
            log.error("Error al escribir archivo en el workspace", e);
            throw new RuntimeException("Error al escribir archivo: " + relativePath, e);
        }
    }

    @Override
    public String readFile(String relativePath) {
        Path targetPath = workspaceRoot.resolve(relativePath);
        try {
            if (!Files.exists(targetPath)) {
                return "";
            }
            return Files.readString(targetPath);
        } catch (IOException e) {
            log.error("Error al leer archivo en el workspace", e);
            throw new RuntimeException("Error al leer archivo: " + relativePath, e);
        }
    }

    @Override
    public Map<String, String> listFiles(String directoryPattern) {
        Map<String, String> files = new HashMap<>();
        Path searchDir = workspaceRoot.resolve(directoryPattern);
        if (!Files.exists(searchDir) || !Files.isDirectory(searchDir)) {
            return files;
        }

        try (Stream<Path> stream = Files.walk(searchDir)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                try {
                    String relative = workspaceRoot.relativize(path).toString();
                    String content = Files.readString(path);
                    files.put(relative, content);
                } catch (IOException e) {
                    log.error("Error al listar archivo {}", path, e);
                }
            });
        } catch (IOException e) {
            log.error("Error al caminar el workspace", e);
        }
        return files;
    }

    @Override
    public boolean executeBuildAndTest() {
        log.info("Ejecutando compilación y pruebas deterministas (mvn clean test)...");
        try {
            Path backendDir = workspaceRoot.resolve("backend");
            ProcessBuilder builder = new ProcessBuilder();
            builder.directory(backendDir.toFile());
            
            builder.command("mvn", "clean", "test");
            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("[MVN LOG]: {}", line);
                }
            }

            int exitCode = process.waitFor();
            log.info("Compilación finalizada con código de salida: {}", exitCode);
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            log.error("Error al ejecutar proceso de verificación de build", e);
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
