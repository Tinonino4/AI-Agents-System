package com.swfactory.sdlc.infrastructure.ai.springai;

import com.swfactory.sdlc.domain.agent.AgentNode;
import com.swfactory.sdlc.domain.model.AgentTask;
import com.swfactory.sdlc.domain.model.BuildResult;
import com.swfactory.sdlc.domain.model.ProjectContext;
import com.swfactory.sdlc.domain.repository.WorkspaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Adaptador de Infraestructura. Implementa el agente de QA (Calidad / Adversario).
 * Genera los tests JUnit en el workspace y ejecuta el compilador para validar el código.
 */
@Component
public class SpringAiQaAgent implements AgentNode {

    private static final Logger log = LoggerFactory.getLogger(SpringAiQaAgent.class);

    private final ChatClient chatClient;
    private final WorkspaceRepository workspaceRepository;
    private final AgentContextReader agentContextReader;

    public SpringAiQaAgent(ChatClient.Builder chatClientBuilder, WorkspaceRepository workspaceRepository, AgentContextReader agentContextReader) {
        this.workspaceRepository = workspaceRepository;
        this.agentContextReader = agentContextReader;
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        Eres el Ingeniero de QA de una factoría de software autónoma.
                        Tu rol es escribir tests JUnit 5 de calidad y desafiar el código generado.
                        
                        Tienes a tu disposición una herramienta para leer el contexto del proyecto (.agents/blueprint.md, roadmap.md, etc.) si necesitas alinear tus estrategias de prueba con el roadmap o las reglas técnicas del sistema.
                        
                        IMPORTANTE: Debes dar tu respuesta estructurada para escribir archivos usando este formato:
                        === FILE: nombre_relativo_del_archivo ===
                        [contenido del archivo]
                        === END FILE ===
                        """)
                .defaultTools(agentContextReader)
                .build();
    }

    @Override
    public String getRole() {
        return "QA";
    }

    @Override
    public AgentTask execute(AgentTask task, ProjectContext context) {
        String projectDir = getProjectDirName(context);
        log.info("Agente QA generando pruebas unitarias en el workspace para la tarea: {}", task.getTitle());
        String code = context.getPhaseOutputs().getOrDefault("DEVELOPMENT", "No hay código backend.");

        // Obtener archivos existentes en el proyecto para dar contexto de pruebas
        Map<String, String> existingFiles = workspaceRepository.listFiles(projectDir, "");
        StringBuilder filesContext = new StringBuilder();
        if (!existingFiles.isEmpty()) {
            filesContext.append("\n=== ARCHIVOS EXISTENTES EN EL PROYECTO (Desarrollo y Tests previos) ===\n");
            existingFiles.forEach((path, content) -> {
                filesContext.append("=== FILE: ").append(path).append(" ===\n")
                        .append(content).append("\n=== END FILE ===\n\n");
            });
        }

        String promptInput = """
                Analiza el código backend e implementa o actualiza la correspondiente clase de pruebas unitarias:
                %s
                
                %s
                
                Utiliza la estructura de delimitación para escribir los archivos de pruebas.
                """.formatted(code, filesContext.toString());

        try {
            String response = chatClient.prompt()
                    .user(promptInput)
                    .call()
                    .content();

            // Escribir pruebas en el workspace
            parseAndWriteFiles(response, projectDir);

            // Ejecutar maven test en el sandbox/sistema de archivos local
            BuildResult buildResult = workspaceRepository.executeBuildAndTest(projectDir);

            if (buildResult.success()) {
                log.info("Verificación de QA: Compilación y Pruebas exitosas (BUILD SUCCESS).");
                task.setStatus("COMPLETED");
                task.setOutputData(response);
            } else {
                log.warn("Verificación de QA: Compilación o Pruebas fallidas.");
                task.setStatus("FAILED");
                // Guardar los logs de compilación reales para que el orquestador se los pase al desarrollador
                task.setOutputData(buildResult.logs());
            }
        } catch (Exception e) {
            log.error("Error en verificación de QA", e);
            task.setStatus("FAILED");
            task.setOutputData("Fallo en ejecución de QA: " + e.getMessage());
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
                log.info("Archivo escrito por QaAgent en el workspace ({}): {}", projectDir, relativePath);
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
