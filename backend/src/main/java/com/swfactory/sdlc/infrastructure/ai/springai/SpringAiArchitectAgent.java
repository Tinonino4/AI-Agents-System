package com.swfactory.sdlc.infrastructure.ai.springai;

import com.swfactory.sdlc.domain.agent.AgentNode;
import com.swfactory.sdlc.domain.model.AgentTask;
import com.swfactory.sdlc.domain.model.ProjectContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Adaptador de Infraestructura. Implementa el agente de Arquitectura Técnica (Architect)
 * utilizando el ChatClient de Spring AI para interactuar con los LLMs.
 */
@Component
public class SpringAiArchitectAgent implements AgentNode {

    private static final Logger log = LoggerFactory.getLogger(SpringAiArchitectAgent.class);

    private final ChatClient chatClient;

    /**
     * Constructor que inyecta el constructor del ChatClient.
     * Define el System Prompt inicial de la personalidad del Arquitecto Técnico.
     */
    public SpringAiArchitectAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        Eres el Agente Arquitecto Técnico de una factoría de software autónoma.
                        Tu responsabilidad es definir la arquitectura base, bounded contexts, esquemas de bases de datos
                        y contratos OpenAPI a partir de las Historias de Usuario provistas en formato Gherkin.
                        Tus decisiones deben seguir buenas prácticas como DDD, Arquitectura Hexagonal y seguridad.
                        Entrega siempre salidas altamente detalladas y estructuradas en Markdown.
                        
                        CRÍTICO: Al generar tablas en Markdown, detalla únicamente las filas con datos reales. Evita por completo generar filas vacías repetitivas o bucles interminables de tuberías ('|'). Finaliza la respuesta de manera limpia y concisa en cuanto la documentación esté completa.
                        """)
                .build();
    }

    @Override
    public String getRole() {
        return "ARCHITECT";
    }

    @Override
    public AgentTask execute(AgentTask task, ProjectContext context) {
        log.info("Agente Arquitecto Técnico ejecutando tarea: {}", task.getTitle());

        String inputStories = context.getPhaseOutputs().getOrDefault("ANALYSIS", "No hay especificación de requisitos disponible.");

        String promptInput = """
                Analiza las siguientes historias de usuario en Gherkin y genera la especificación
                de arquitectura técnica del sistema, incluyendo:
                1. Mapeo de Bounded Contexts y Entidades.
                2. Contrato OpenAPI 3.1 para la API REST.
                3. Esquema SQL para PostgreSQL.
                
                Requisitos a analizar:
                %s
                """.formatted(inputStories);

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
}
