package com.swfactory.sdlc.infrastructure.ai.springai;

import com.swfactory.sdlc.domain.agent.AgentNode;
import com.swfactory.sdlc.domain.model.AgentTask;
import com.swfactory.sdlc.domain.model.ProjectContext;
import com.swfactory.sdlc.domain.repository.WorkspaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Adaptador de Infraestructura. Implementa el agente de Desarrollo Backend (BACKEND).
 * Escribe el código de negocio directamente en el espacio de trabajo físico del proyecto.
 */
@Component
public class SpringAiBackendAgent implements AgentNode {

    private static final Logger log = LoggerFactory.getLogger(SpringAiBackendAgent.class);

    private final ChatClient chatClient;
    private final WorkspaceRepository workspaceRepository;
    private final AgentContextReader agentContextReader;

    public SpringAiBackendAgent(ChatClient.Builder chatClientBuilder,
                                WorkspaceRepository workspaceRepository,
                                AgentContextReader agentContextReader,
                                @Value("${app.workspace.root}") String workspaceRootPath) {
        this.workspaceRepository = workspaceRepository;
        this.agentContextReader = agentContextReader;

        String skillContext = "";
        try {
            Path skillPath = Paths.get(workspaceRootPath).resolve(".github/skills/springboot-backend/SKILL.md");
            if (Files.exists(skillPath)) {
                skillContext = "\n\n=== REGLAS TÉCNICAS Y CONVENCIONES DE BACKEND (SKILL.md) ===\n" 
                        + Files.readString(skillPath);
                log.info("Archivo SKILL.md cargado exitosamente en SpringAiBackendAgent.");
            } else {
                log.warn("Archivo SKILL.md no encontrado en la ruta: {}. Se continuará sin estas reglas.", skillPath);
            }
        } catch (Exception e) {
            log.error("No se pudo cargar el archivo SKILL.md en el agente backend", e);
        }

        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        Eres el Ingeniero de Software Backend de una factoría de software autónoma.
                        Tu rol es escribir la implementación Java (Spring Boot) basada en la arquitectura provista.
                        
                        Tienes a tu disposición una herramienta para leer el contexto del proyecto (.agents/blueprint.md, roadmap.md, etc.) si necesitas consultar las decisiones del sistema, estándares de prompt o el estado actual de desarrollo.
                        
                        IMPORTANTE: Debes dar tu respuesta estructurada para escribir archivos usando este formato:
                        === FILE: nombre_relativo_del_archivo ===
                        [contenido del archivo]
                        === END FILE ===
                        """ + skillContext)
                .defaultTools(agentContextReader)
                .build();
    }

    @Override
    public String getRole() {
        return "BACKEND";
    }

    @Override
    public AgentTask execute(AgentTask task, ProjectContext context) {
        String projectDir = getProjectDirName(context);
        log.info("Agente Backend escribiendo código en el workspace para la tarea: {}", task.getTitle());
        
        String inputSpec = task.getInputData();

        // Obtener archivos existentes en el proyecto para dar contexto de código
        Map<String, String> existingFiles = workspaceRepository.listFiles(projectDir, "");
        StringBuilder filesContext = new StringBuilder();
        if (!existingFiles.isEmpty()) {
            filesContext.append("\n=== ARCHIVOS EXISTENTES EN EL PROYECTO (Desarrolla o modifica sobre estos archivos) ===\n");
            existingFiles.forEach((path, content) -> {
                filesContext.append("=== FILE: ").append(path).append(" ===\n")
                        .append(content).append("\n=== END FILE ===\n\n");
            });
        }

        String promptInput = """
                Basándote en la especificación técnica / feedback de errores:
                %s
                
                %s
                
                Genera las clases necesarias de dominio, controladores o modifica los archivos existentes.
                Utiliza la estructura de delimitación para escribir los archivos.
                """.formatted(inputSpec, filesContext.toString());

        try {
            String response = chatClient.prompt()
                    .user(promptInput)
                    .call()
                    .content();

            // Analizar respuesta y escribir los archivos en disco
            parseAndWriteFiles(response, projectDir);
            
            task.setOutputData(response);
            log.info("Código backend generado y escrito en el espacio de trabajo.");
        } catch (Exception e) {
            log.error("Error en agente Backend", e);
            task.setOutputData("Error al invocar LLM o escribir archivos: " + e.getMessage());
        }

        return task;
    }

    private void parseAndWriteFiles(String response, String projectDir) {
        if (response == null || response.isEmpty()) return;

        String[] blocks = response.split("=== FILE: ");
        for (int i = 1; i < blocks.length; i++) {
            String block = blocks[i];
            int pathEnd = block.indexOf(" ===");
            if (pathEnd == -1) {
                pathEnd = block.indexOf("\n");
            }
            if (pathEnd == -1) continue;

            String relativePath = block.substring(0, pathEnd).trim();
            
            int codeStart = block.indexOf("\n", pathEnd) + 1;
            int codeEnd = block.indexOf("=== END FILE ===");
            if (codeEnd == -1) {
                codeEnd = block.length();
            }

            if (codeStart >= 0 && codeStart < codeEnd) {
                String content = block.substring(codeStart, codeEnd).trim();
                workspaceRepository.writeFile(projectDir, relativePath, content);
                log.info("Archivo escrito por BackendAgent en el workspace ({}): {}", projectDir, relativePath);
            }
        }
    }

    private String getProjectDirName(ProjectContext context) {
        if (context == null || context.getName() == null || context.getName().isBlank()) {
            return "default-project";
        }
        return context.getName().toLowerCase()
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-z0-9_-]", "");
    }
}
