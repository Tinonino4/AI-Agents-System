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
 * Adaptador de Infraestructura. Implementa el agente de Arquitectura Técnico (Architect)
 * utilizando el ChatClient de Spring AI para interactuar con los LLMs.
 */
@Component
public class SpringAiArchitectAgent implements AgentNode {

    private static final Logger log = LoggerFactory.getLogger(SpringAiArchitectAgent.class);

    private final ChatClient chatClient;
    private final WorkspaceRepository workspaceRepository;
    private final AgentContextReader agentContextReader;

    /**
     * Constructor que inyecta el constructor del ChatClient, el repositorio del Workspace,
     * el lector de contexto y la ruta raíz del workspace para cargar las reglas técnicas (SKILL.md).
     * Define el System Prompt inicial de la personalidad del Arquitecto Técnico.
     */
    public SpringAiArchitectAgent(ChatClient.Builder chatClientBuilder,
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
                log.info("Archivo SKILL.md cargado exitosamente en SpringAiArchitectAgent.");
            } else {
                log.warn("Archivo SKILL.md no encontrado en la ruta: {}. Se continuará sin estas reglas.", skillPath);
            }
        } catch (Exception e) {
            log.error("No se pudo cargar el archivo SKILL.md en el agente arquitecto", e);
        }

        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        Eres el Agente Arquitecto Técnico de una factoría de software autónoma.
                        Tu responsabilidad es definir la arquitectura base, bounded contexts, esquemas de bases de datos
                        y contratos OpenAPI a partir de las Historias de Usuario provistas en formato Gherkin.
                        Tus decisiones deben seguir buenas prácticas como DDD, Arquitectura Hexagonal y seguridad.
                        Entrega siempre salidas altamente detalladas y estructuradas en Markdown.
                        
                        Tienes a tu disposición una herramienta para leer el contexto del proyecto (.agents/blueprint.md, roadmap.md, decisions.md, etc.) si necesitas consultar las decisiones arquitectónicas pasadas, principios o reglas técnicas.
                        
                        CRÍTICO: Al generar tablas en Markdown, detalla únicamente las filas con datos reales. Evita por completo generar filas vacías repetitivas o bucles interminables de tuberías ('|'). Finaliza la respuesta de manera limpia y concisa en cuanto la documentación esté completa.
                        """ + skillContext)
                .defaultTools(agentContextReader)
                .build();
    }

    @Override
    public String getRole() {
        return "ARCHITECT";
    }

    @Override
    public AgentTask execute(AgentTask task, ProjectContext context) {
        log.info("Agente Arquitecto Técnico ejecutando tarea: {}", task.getTitle());
        String projectDir = getProjectDirName(context);

        String inputStories = context.getPhaseOutputs().getOrDefault("ANALYSIS", "No hay especificación de requisitos disponible.");

        // Obtener archivos existentes en el proyecto para dar contexto de arquitectura
        Map<String, String> existingFiles = workspaceRepository.listFiles(projectDir, "");
        StringBuilder filesContext = new StringBuilder();
        if (!existingFiles.isEmpty()) {
            filesContext.append("\n=== ARCHIVOS EXISTENTES EN EL PROYECTO (Considéralos al definir la arquitectura) ===\n");
            existingFiles.forEach((path, content) -> {
                filesContext.append("=== FILE: ").append(path).append(" ===\n")
                        .append(content).append("\n=== END FILE ===\n\n");
            });
        }

        String promptInput = """
                Analiza las siguientes historias de usuario en Gherkin y genera la especificación
                de arquitectura técnica del sistema, incluyendo:
                1. Mapeo de Bounded Contexts y Entidades.
                2. Contrato OpenAPI 3.1 para la API REST.
                3. Esquema SQL para PostgreSQL.
                
                Requisitos a analizar:
                %s
                
                %s
                
                Si el proyecto ya tiene una arquitectura establecida (ver archivos existentes),
                propon los cambios o adiciones necesarias sobre la estructura actual en lugar de sobrescribirla por completo.
                """.formatted(inputStories, filesContext.toString());

        try {
            // Invocar el LLM usando la API fluent de ChatClient de Spring AI
            String response = chatClient.prompt()
                    .user(promptInput)
                    .call()
                    .content();

            task.setOutputData(response);
            log.info("Especificación de arquitectura técnica generada con éxito.");
        } catch (Exception e) {
            log.error("Error al invocar el LLM para el Agente Arquitecto", e);
            task.setOutputData("Error de ejecución del LLM: " + e.getMessage());
        }

        return task;
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
