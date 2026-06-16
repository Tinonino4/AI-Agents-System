package com.swfactory.sdlc.infrastructure.ai.springai;

import com.swfactory.sdlc.domain.agent.AgentNode;
import com.swfactory.sdlc.domain.model.AgentTask;
import com.swfactory.sdlc.domain.model.ProjectContext;
import com.swfactory.sdlc.domain.repository.WorkspaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Adaptador de Infraestructura. Implementa el agente de QA (Calidad / Adversario).
 * Genera los tests JUnit en el workspace y ejecuta el compilador para validar el código.
 */
@Component
public class SpringAiQaAgent implements AgentNode {

    private static final Logger log = LoggerFactory.getLogger(SpringAiQaAgent.class);

    private final ChatClient chatClient;
    private final WorkspaceRepository workspaceRepository;

    public SpringAiQaAgent(ChatClient.Builder chatClientBuilder, WorkspaceRepository workspaceRepository) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        Eres el Ingeniero de QA de una factoría de software autónoma.
                        Tu rol es escribir tests JUnit 5 de calidad y desafiar el código generado.
                        
                        IMPORTANTE: Debes dar tu respuesta estructurada para escribir archivos usando este formato:
                        === FILE: nombre_relativo_del_archivo ===
                        [contenido del archivo]
                        === END FILE ===
                        """)
                .build();
        this.workspaceRepository = workspaceRepository;
    }

    @Override
    public String getRole() {
        return "QA";
    }

    @Override
    public AgentTask execute(AgentTask task, ProjectContext context) {
        log.info("Agente QA generando pruebas unitarias en el workspace para la tarea: {}", task.getTitle());
        String code = context.getPhaseOutputs().getOrDefault("DEVELOPMENT", "No hay código backend.");

        String promptInput = """
                Analiza el código backend e implementa la correspondiente clase de pruebas unitarias:
                %s
                
                Utiliza la estructura de delimitación para escribir los archivos.
                """.formatted(code);

        try {
            String response = chatClient.prompt()
                    .user(promptInput)
                    .call()
                    .content();

            // Escribir pruebas en el workspace
            parseAndWriteFiles(response);

            // Ejecutar maven test en el sandbox/sistema de archivos local
            boolean buildPassed = workspaceRepository.executeBuildAndTest();

            if (buildPassed) {
                log.info("Verificación de QA: Compilación y Pruebas exitosas (BUILD SUCCESS).");
                task.setStatus("COMPLETED");
                task.setOutputData(response);
            } else {
                log.warn("Verificación de QA: Compilación o Pruebas fallidas.");
                task.setStatus("FAILED");
                // Si falla, pasamos detalles del error o un mock del log si es una simulación inicial
                task.setOutputData("[ERROR] Test compilation failed at package com.swfactory.sdlc. \n" +
                                   "Mismatched signature or assertion failed in ProductControllerTest.java line 24.");
            }
        } catch (Exception e) {
            log.error("Error en verificación de QA", e);
            task.setStatus("FAILED");
            task.setOutputData("Fallo en ejecución de QA: " + e.getMessage());
        }

        return task;
    }

    private void parseAndWriteFiles(String response) {
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
                workspaceRepository.writeFile(relativePath, content);
                log.info("Archivo escrito por QaAgent en el workspace: {}", relativePath);
            }
        }
    }
}
