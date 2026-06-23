package com.swfactory.sdlc.infrastructure.ai.springai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Adaptador de infraestructura que expone herramientas a los LLM (Spring AI)
 * para leer los ficheros de contexto del proyecto en la carpeta `.agents/`.
 */
@Component
public class AgentContextReader {

    private static final Logger log = LoggerFactory.getLogger(AgentContextReader.class);
    private final Path agentsDirectory;

    public AgentContextReader(@Value("${app.workspace.root}") String workspaceRootPath) {
        this.agentsDirectory = Paths.get(workspaceRootPath).resolve(".agents");
    }

    @Tool(description = "Lee un archivo de contexto de la carpeta .agents (opciones: blueprint.md, roadmap.md, session.md, insights.md, agents.md, decisions.md) para conocer las reglas, roadmap o el estado actual del desarrollo.")
    public String readAgentContextFile(String fileName) {
        // Sanitizar el nombre del archivo para evitar Directory Traversal
        String safeName = Paths.get(fileName).getFileName().toString();
        if (!safeName.endsWith(".md")) {
            safeName += ".md";
        }
        
        Path filePath = agentsDirectory.resolve(safeName);
        if (!Files.exists(filePath)) {
            log.warn("Se intentó leer el archivo de contexto {} pero no existe en {}", safeName, filePath);
            return "Error: El archivo " + safeName + " no existe en el directorio de contexto.";
        }

        try {
            log.info("Agente leyendo archivo de contexto: {}", safeName);
            return Files.readString(filePath);
        } catch (Exception e) {
            log.error("Error leyendo archivo de contexto {}", safeName, e);
            return "Error al leer el archivo: " + e.getMessage();
        }
    }
}
